package top.rymc.phira.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Getter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import top.rymc.phira.main.command.CommandService;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.ServerChannelInitializer;
import top.rymc.phira.main.network.request.PhiraFetcher;
import top.rymc.phira.main.storage.DatabaseManager;
import top.rymc.phira.main.storage.dao.GameRecordDao;
import top.rymc.phira.main.storage.dao.GameRecordDaoImpl;
import top.rymc.phira.main.storage.player.PlayerDataManager;
import top.rymc.phira.main.storage.player.PlayerInfo;
import top.rymc.phira.main.storage.player.VerificationCodeService;
import top.rymc.phira.main.storage.player.VerifyResult;
import top.rymc.phira.main.storage.player.VerifyServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    private static final int[] chartList = {42241, 51695, 15451};

    @Getter
    private static final Logger logger = LogManager.getLogger("Main");

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicLong startTime = new AtomicLong(0);

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
    }

    private static NioEventLoopGroup bossGroup;
    private static NioEventLoopGroup workerGroup;
    private static Channel serverChannel;
    private static final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Getter
    private static DatabaseManager dbManager;
    @Getter
    private static GameRecordDao dao;
    @Getter
    private static List<ChartInfo> chartInfoList;
    @Getter
    private static VerificationCodeService verificationCodeService;
    @Getter
    private static VerifyServer verifyServer;

    public static boolean isRunning() {
        return running.get();
    }

    public static void main(String[] args) {
        long bootStart = System.nanoTime();

        try {
            start(args, bootStart);
            awaitShutdown();
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        }
    }

    public static void start(String[] args, long bootStart) throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running");
        }

        startTime.set(System.currentTimeMillis());
        logger.info("Phira Server is starting...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("ShutdownHook");
            if (isRunning()) {
                shutdown();
            }
        }, "shutdown-hook"));

        initializeVerificationCodeService();
        initializeBusinessLogic();

        int port = parsePort(args);

        initializeNetwork(port);

        new CommandService().start();
        long totalTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - bootStart);
        logger.info("Done ({}s)!", String.format("%.3f", totalTime / 1000.0));
    }

    private static void initializeBusinessLogic() {
        logger.info("Loading chart information...");
        chartInfoList = Arrays.stream(chartList)
                .mapToObj(PhiraFetcher.GET_CHART_INFO.toUncheckedIntFunction())
                .toList();
        logger.info("Loaded {} chart(s)", chartInfoList.size());

        logger.info("Initializing database...");
        dbManager = new DatabaseManager("record-data");
        dao = new GameRecordDaoImpl(dbManager);
        logger.info("Database initialized");
    }

    private static void initializeVerificationCodeService() throws IOException {
        logger.info("Initializing verification code service...");
        verificationCodeService = new VerificationCodeService();
        verifyServer = new VerifyServer("127.0.0.1", 18080, Main::verifyCode, Main::check);
        logger.info("Starting verification server on {}", verifyServer.getAddress());
        verifyServer.start();
        logger.info("Verification code service initialized");
    }

    private static Optional<VerifyResult> verifyCode(String qq, String code) {
        Optional<UserInfo> userInfoOpt = verificationCodeService.validateAndConsume(code);
        if (userInfoOpt.isEmpty()) {
            return Optional.empty();
        }

        UserInfo userInfo = userInfoOpt.get();
        PlayerInfo playerInfo = new PlayerInfo(userInfo.getId(), qq, false, 5);
        PlayerDataManager.savePlayerInfo(playerInfo);
        return Optional.of(new VerifyResult(userInfo.getId(), userInfo.getName()));
    }

    private static Optional<VerifyResult> check(String qq) {
        return PlayerDataManager.getAllCached().stream()
                .filter(p -> p.getQq().equals(qq))
                .findFirst()
                .map(p -> {
                    UserInfo userInfo = PhiraFetcher.GET_USER_INFO_BY_ID.toIntFunction((e) -> null).apply(p.getId());
                    String name = userInfo != null ? userInfo.getName() : "#用户名查询失败#";
                    return new VerifyResult(p.getId(), name);
                });
    }

    private static int parsePort(String[] args) {
        OptionParser parser = new OptionParser();
        parser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(12346);
        OptionSet options = parser.parse(args);
        return (Integer) options.valueOf("port");
    }

    private static void initializeNetwork(int port) throws Exception {
        logger.info("Initializing network...");

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("Netty-Boss", true));
        workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("Netty-Worker", true));

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerChannelInitializer(allChannels));

        InetAddress ipv4 = InetAddress.getByName("0.0.0.0");
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(ipv4, port)).sync();

        serverChannel = future.channel();
        allChannels.add(serverChannel);

        logger.info("Listening on: {}:{}", ipv4.getHostAddress(), port);
    }

    public static void awaitShutdown() {
        if (serverChannel == null) return;

        try {
            logger.info("Server is running. Type 'stop' to stop.");
            serverChannel.closeFuture().await();
        } catch (InterruptedException e) {
            logger.info("Server thread interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }

    public static void shutdown() {
        if (!running.compareAndSet(true, false)) return;

        long shutdownStart = System.nanoTime();
        int channelCount = allChannels.size();

        logger.info("Shutting down...");

        if (dbManager != null) {
            logger.info("Closing database...");
            dbManager.shutdown();
        }

        if (channelCount > 0) {
            logger.info("Closing {} channel(s)...", channelCount);
            allChannels.close().awaitUninterruptibly(10, TimeUnit.SECONDS);
            logger.info("Channels closed");
        }

        if (workerGroup != null) {
            logger.info("Shutting down worker group...");
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
        }
        if (bossGroup != null) {
            logger.info("Shutting down boss group...");
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly(5, TimeUnit.SECONDS);
        }

        long uptime = System.currentTimeMillis() - startTime.get();
        long shutdownDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - shutdownStart);

        logger.info("Uptime: {}m {}s",
                TimeUnit.MILLISECONDS.toMinutes(uptime),
                TimeUnit.MILLISECONDS.toSeconds(uptime) % 60);
        logger.info("Shutdown completed in {}ms. Goodbye!", shutdownDuration);

        LogManager.shutdown();
    }
}