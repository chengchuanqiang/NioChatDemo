package com.mychat;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端
 *
 * @author ccq
 */
public class ChatServer extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    /******************按钮名称定义*********************/
    private static final String START_SERVER = "启动服务";
    private static final String STOP_SERVER = "断开服务";
    private static final String SEND = "发送";
    private static final String CLEAR_CONTENT = "清除历史";
    private static final String SEPARATOR = "  ";

    /******************命令约定*********************/
    private static final String LOGIN_COMMAND = "login";                            // 登录
    private static final String CHAT_COMMAND = "chat";                                // 聊天
    private static final String ALL_USER_COMMAND = "allUser";                        // 发送所有用户
    private static final String OFFLINE_USE_COMMAND = "offlineUser";                // 用户下线
    private static final String ONLINE_USER_COMMAND = "onlineUser";                    // 用户上线
    private static final String ONLINE_USERLIST_COMMAND = "onlineUserList";            // 所有在线用户
    private static final String SERVER_STOP = "serverStop";                            // 服务端断开


    //
    private JLabel ipLabel;
    private JLabel portLabel;

    // text Field
    private JTextField ipField;
    private JTextField portField;
    private JTextField chatContentField;

    // area
    private JList<String> friendList;
    private DefaultListModel<String> listModel;
    private JTextArea historyRecordArea;

    //buttons
    private JButton startServerBtn;
    private JButton stopServerBtn;
    private JButton sendBtn;
    private JButton clearContentBtn;

    //
    private JPanel settingPanel;
    private JPanel chatPanel;
    private JPanel contentPanel;

    // 保存在线用户列表
    private Map<String, User> userMap = new ConcurrentHashMap<String, User>();

    // 服务器ip和端口
    private InetAddress hostAddress;
    private int port;

    // 服务端通道
    private ServerSocketChannel serverSocketChannel;

    // 缓存区
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    // 选择器
    private Selector selector;

    // 服务端线程
    private Thread serverThread;

    /**
     * 服务端构造方法，用于初始化页面组件和绑定按钮监听器
     */
    public ChatServer() {
        initComponents();
        setupListener();
    }

    /**
     * 初始化页面组件
     */
    private void initComponents() {

        /******************用户信息和连接配置*********************/
        settingPanel = new JPanel();
        settingPanel.setBorder(new TitledBorder("服务器配置"));
        settingPanel.setLayout(new GridLayout(1, 6, 5, 10));


        /******************配置信息设置*********************/
        ipField = new JTextField("127.0.0.1");
        portField = new JTextField("9090");

        ipLabel = new JLabel("服务端ip：");
        portLabel = new JLabel("服务端端口：");

        startServerBtn = new JButton(START_SERVER);
        stopServerBtn = new JButton(STOP_SERVER);


        /******************将组件添加到配置中*********************/
        settingPanel.add(ipLabel);
        settingPanel.add(ipField);
        settingPanel.add(portLabel);
        settingPanel.add(portField);
        settingPanel.add(startServerBtn);
        settingPanel.add(stopServerBtn);


        /******************左边的在线用户*********************/
        listModel = new DefaultListModel<String>();
        friendList = new JList<String>(listModel);
        JScrollPane leftScroll = new JScrollPane(friendList);
        leftScroll.setBorder(new TitledBorder("在线用户"));


        /******************右边的历史消息显示和发送消息*********************/
        chatPanel = new JPanel(new BorderLayout());

        contentPanel = new JPanel(new BorderLayout());
        chatContentField = new JTextField();
        sendBtn = new JButton(SEND);

        clearContentBtn = new JButton(CLEAR_CONTENT);
        contentPanel.add(chatContentField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        btnPanel.add(sendBtn);
        btnPanel.add(clearContentBtn);


        contentPanel.add(chatContentField, BorderLayout.CENTER);
        contentPanel.add(btnPanel, BorderLayout.EAST);
        contentPanel.setBorder(new TitledBorder("发送消息"));

        historyRecordArea = new JTextArea();
        historyRecordArea.setForeground(Color.blue);
        historyRecordArea.setEditable(false);

        chatPanel.add(historyRecordArea, BorderLayout.CENTER);
        chatPanel.add(contentPanel, BorderLayout.SOUTH);

        JScrollPane rightScroll = new JScrollPane(chatPanel);
        rightScroll.setBorder(new TitledBorder("消息显示区"));


        /******************设置左右显示定位*********************/
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        centerSplit.setDividerLocation(100);


        /******************设置主体定位*********************/
        getContentPane().add(settingPanel, BorderLayout.NORTH);
        getContentPane().add(centerSplit, BorderLayout.CENTER);

        /******************初始化按钮和文本框状态*********************/
        initBtnAndTextConnect();

        /******************设置窗体大小和居中显示*********************/
        this.setTitle("服务器");
        this.setSize(800, 500);
        this.setLocationRelativeTo(this.getOwner());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

    }

    /**
     * 设置按钮的监听事件
     */
    private void setupListener() {
        startServerBtn.addActionListener(this);
        stopServerBtn.addActionListener(this);
        sendBtn.addActionListener(this);
        clearContentBtn.addActionListener(this);
        // 发送消息的文本框回车事件
        chatContentField.addActionListener(this);
    }

    // 用于监听按钮的点击事件
    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (START_SERVER.equals(actionCommand)) {

            try {
                // 服务启动
                String serverIp = ipField.getText();
                String portStr = portField.getText();

                if (StringUtils.isEmpty(serverIp) || StringUtils.isEmpty(portStr)) {
                    JOptionPane.showMessageDialog(this, "请输入服务器ip和端口号!");
                    return;
                }
                // 初始化连接信息
                initConnection(InetAddress.getLocalHost(), Integer.parseInt(portStr));
                connect();

                setTitle("服务器 - " + hostAddress.getHostAddress());
            } catch (NumberFormatException e1) {
                JOptionPane.showMessageDialog(this, "端口输入异常，请输入数字（如：8080）", "错误", JOptionPane.ERROR_MESSAGE);
                //e1.printStackTrace();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "服务启动失败！" + e1.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                //e1.printStackTrace();
            }

        } else if (STOP_SERVER.equals(actionCommand)) {
            //　关闭服务器
            this.shutdown(serverThread);

        } else if (SEND.equals(actionCommand) || e.getSource() == chatContentField) {
            //发送按钮和文本框回车事件
            String message = chatContentField.getText();
            chatContentField.setText("");

            if (message == null || message.equals("")) {
                JOptionPane.showMessageDialog(this, "消息不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String toUserName = this.getSelectedUser();

            if (toUserName.equals(ALL_USER_COMMAND)) {
                historyRecordArea.append(formatMessage("对所有人说：" + message));
            } else {
                historyRecordArea.append(formatMessage("对 " + toUserName + " 说：" + message));
            }

            try {
                this.sendMsgToUser(toUserName, message);
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(this, "消息发送失败" + e1.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                //e1.printStackTrace();
            }
        } else if (CLEAR_CONTENT.equals(actionCommand)) {
            // 清空历史聊天记录
            historyRecordArea.setText("");
        }
    }

    //　初始化页面按钮设置
    public void initBtnAndTextConnect() {
        startServerBtn.setEnabled(true);
        //ipField.setEnabled(true);
        ipField.setEnabled(false);
        portField.setEnabled(true);

        stopServerBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        //clearContentBtn.setEnabled(false);
        chatContentField.setEnabled(false);
    }

    // 启动成功按钮设置
    public void showBtnAndTextConnectSuccess() {

        stopServerBtn.setEnabled(true);
        sendBtn.setEnabled(true);
        //clearContentBtn.setEnabled(true);
        chatContentField.setEnabled(true);

        startServerBtn.setEnabled(false);
        ipField.setEnabled(false);
        portField.setEnabled(false);
    }

    // 初始化连接信息
    public void initConnection(InetAddress hostAddress, int port) throws IOException {
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = initSelector();
    }

    // 启动服务器服务
    public void connect() {
        showBtnAndTextConnectSuccess();
        historyRecordArea.append(formatMessage("聊天服务器启动成功..."));
        serverThread = new ServerThread();
        serverThread.setName("聊天服务器");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    // 获取选中的人员
    public String getSelectedUser() {
        int index = friendList.getSelectedIndex();
        if (index >= 0) {
            String userName = friendList.getSelectedValue();
            return userName;
        }
        return ALL_USER_COMMAND;
    }

    // 关闭服务器
    private void shutdown(Thread serverThread) {
        try {
            // 所有的用户下线
            Message message = new Message();
            message.setCommand(SERVER_STOP);
            sendAllUserMessage(message);

            // 清空在线用户列表
            userMap.clear();
            listModel.clear();

            serverThread.interrupt();
            selector.close();
            serverSocketChannel.close();

            historyRecordArea.append(formatMessage("服务器成功关闭"));
            initBtnAndTextConnect();
        } catch (IOException e) {
            historyRecordArea.append(formatMessage("关闭服务器异常" + e.getMessage()));
            e.printStackTrace();
            return;
        }
    }

    // 所有的线程
    public void getAllThread() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        Set<Entry<Thread, StackTraceElement[]>> entrySet = allStackTraces.entrySet();

        for (Entry<Thread, StackTraceElement[]> entry : entrySet) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    // 初始化选择器
    private Selector initSelector() throws IOException {
        this.selector = SelectorProvider.provider().openSelector();

        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        InetSocketAddress inetSocketAddress = new InetSocketAddress(this.hostAddress, this.port);

        historyRecordArea.append(formatMessage("服务器信息 : " + inetSocketAddress.getAddress() + " : " + inetSocketAddress.getPort()));

        serverSocketChannel.bind(inetSocketAddress);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        return selector;
    }

    // 服务器线程，用与监听事件
    class ServerThread extends Thread {
        @Override
        public void run() {
            try {
                while (selector.select() > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            read(key);
                        }
                        iterator.remove();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 接受事件
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        //System.out.println(socketChannel.getRemoteAddress());
        //Socket socket = socketChannel.socket();
        historyRecordArea.append(formatMessage(socketChannel.getRemoteAddress() + " 连接请求"));
        socketChannel.configureBlocking(false);
        socketChannel.register(this.selector, SelectionKey.OP_READ);
        key.interestOps(SelectionKey.OP_ACCEPT);
    }

    // 读取事件
    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.readBuffer.clear();

        int len = 0;
        try {
            len = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            // 远程强制关闭通道，取消选择键并关闭通道
            closeClient(key, socketChannel);
            System.err.println(e);
            return;
        }

        if (len == -1) {
            // 客户端通道调用close进行关闭，取消选择键并关闭通道
            closeClient(key, socketChannel);
            return;
        }

        String msg = new String(this.readBuffer.array(), 0, len, Charset.forName("UTF-8"));

        Message message = (Message) JSONObject.toBean(JSONObject.fromObject(msg), Message.class);
        System.out.println(message.toString());
        String command = message.getCommand();
        String fromUserName = message.getFromUserName();
        String content = message.getContent();
        String toUserName = message.getToUserName();

        Message returnMsg = new Message();

        Message toAllMsg = new Message();

        // 业务逻辑处理
        switch (command) {
            case LOGIN_COMMAND:
                System.out.println(formatMessage("用户 ：" + fromUserName + "请求登录..."));
                String password = PropertyFactory.getProperty(fromUserName);

                if (password == null) {
                    System.out.println(formatMessage("用户：" + fromUserName + "不存在"));
                    returnMsg.setContent("用户不存在");
                    returnMsg.setStatus("MSG_PWD_ERROR");
                    historyRecordArea.append(formatMessage("用户 ：" + fromUserName + "不存在！"));
                } else if (password.equals(content)) {
                    if (!userMap.containsKey(fromUserName)) {
                        System.out.println(formatMessage("用户：" + fromUserName + "登录成功！"));
                        User user = new User(fromUserName, socketChannel);
                        userMap.put(fromUserName, user);
                        returnMsg.setContent("用户：" + fromUserName + "登录成功！");
                        returnMsg.setStatus("MSG_SUCCESS");
                        returnMsg.setFromUserName(fromUserName);

                        listModel.addElement(fromUserName);
                        historyRecordArea.append(formatMessage(fromUserName + " 成功上线！"));

                    } else {
                        System.out.println(formatMessage("该帐号已经登录"));
                        returnMsg.setContent("用户：" + fromUserName + "已经登录！");
                        returnMsg.setStatus("MSG_REPEAT");
                        historyRecordArea.append(formatMessage(fromUserName + " 重复登陆，失败！"));
                    }
                } else {
                    returnMsg.setContent("密码错误");
                    returnMsg.setStatus("MSG_PWD_ERROR");
                    historyRecordArea.append(formatMessage("用户 ：" + fromUserName + "密码错误！"));
                }
                returnMsg.setCommand(LOGIN_COMMAND);
                //发送登录结果
                sendMessage(socketChannel, returnMsg);
                break;
            case CHAT_COMMAND:
                historyRecordArea.append(formatMessage("用户：" + fromUserName + "发消息给用户：" + toUserName + "， 内容是：" + content));
                returnMsg.setCommand(CHAT_COMMAND);
                // 群聊
                if (StringUtils.isNotEmpty(toUserName) && ALL_USER_COMMAND.equals(toUserName)) {
                    returnMsg.setFromUserName(fromUserName);
                    returnMsg.setToUserName(toUserName);
                    returnMsg.setStatus("MSG_SUCCESS");
                    returnMsg.setContent(content);
                    sendAllUserMessage(returnMsg);
                    break;
                }
                // 私聊
                if (userMap.containsKey(fromUserName) && userMap.containsKey(toUserName)
                        && StringUtils.isNotEmpty(content)) {
                    SocketChannel sc = userMap.get(toUserName).getSocketChannel();
                    returnMsg.setFromUserName(fromUserName);
                    returnMsg.setToUserName(toUserName);
                    returnMsg.setStatus("MSG_SUCCESS");
                    returnMsg.setContent(content);
                    sendMessage(sc, returnMsg);
                } else {
                    returnMsg.setFromUserName(fromUserName);
                    returnMsg.setToUserName(toUserName);
                    returnMsg.setStatus("MSG_ERROR");
                    returnMsg.setContent("消息发送失败！");
                    sendMessage(socketChannel, returnMsg);
                }
                break;
            case ONLINE_USERLIST_COMMAND:
                // 通知所有人上线消息
                toAllMsg.setCommand(ONLINE_USER_COMMAND);
                toAllMsg.setFromUserName(fromUserName);
                sendAllMessage(toAllMsg);
        }
    }

    // 发送消息
    private void sendMessage(SocketChannel socketChannel, Message returnMsg) throws IOException {

        JSONObject msg = JSONObject.fromObject(returnMsg);
        if (socketChannel != null && msg != null) {
            byte[] val = msg.toString().getBytes();
            socketChannel.write(ByteBuffer.wrap(val));
        }
    }

    // 用户获取在线用户列表，同时将他上线的消息通知到所有的客户端
    public void sendAllMessage(Message message) throws IOException {

        Message toFromUserMsg = new Message();
        StringBuffer onlineUserName = new StringBuffer();

        // 通知所有人 他上线了
        Set<Entry<String, User>> entrySet = userMap.entrySet();
        for (Entry<String, User> e : entrySet) {
            if (!e.getKey().equals(message.getFromUserName())) {
                JSONObject msg = JSONObject.fromObject(message);
                byte[] val = msg.toString().getBytes();

                e.getValue().getSocketChannel().write(ByteBuffer.wrap(val));

                onlineUserName.append(e.getKey()).append("#");
            }
        }

        // 返回在线用户列表
        if (onlineUserName.length() > 1) {
            String userNames = onlineUserName.substring(0, onlineUserName.length() - 1);
            System.out.println(userNames);
            toFromUserMsg.setContent(userNames);
            toFromUserMsg.setCommand(ONLINE_USERLIST_COMMAND);
            JSONObject msg = JSONObject.fromObject(toFromUserMsg);
            byte[] val = msg.toString().getBytes();
            userMap.get(message.getFromUserName()).getSocketChannel().write(ByteBuffer.wrap(val));
        }


    }

    // 发送给所有在线用户消息
    public void sendAllUserMessage(Message message) throws IOException {
        Set<Entry<String, User>> entrySet = userMap.entrySet();
        for (Entry<String, User> e : entrySet) {
            // 群聊不发给自己
            if (StringUtils.isNotEmpty(message.getFromUserName())
                    && e.getKey().equals(message.getFromUserName())) {
                continue;
            }
            JSONObject msg = JSONObject.fromObject(message);
            byte[] val = msg.toString().getBytes();
            e.getValue().getSocketChannel().write(ByteBuffer.wrap(val));
        }
    }

    // 服务器 聊天发送
    private void sendMsgToUser(String toUserName, String content) throws IOException {
        Message message = new Message();
        message.setCommand(CHAT_COMMAND);
        message.setContent(content);
        message.setStatus("MSG_SUCCESS");
        message.setFromUserName("CCQ服务器");
        message.setToUserName(toUserName);

        if (toUserName.equals(ALL_USER_COMMAND)) {
            // 群发
            sendAllUserMessage(message);
        } else {
            // 单发
            sendMessage(userMap.get(toUserName).getSocketChannel(), message);
        }
    }

    // 用户离线关闭通道
    private void closeClient(SelectionKey sk, SocketChannel sc) throws IOException {
        sk.cancel();
        if (sc != null) {
            for (String key : userMap.keySet()) {
                User user = userMap.get(key);
                if (user.getSocketChannel() == sc) {
                    userMap.remove(key);//从用户列表中移除用户
                    // 用户离线
                    Message msg = new Message();
                    msg.setCommand(OFFLINE_USE_COMMAND);
                    msg.setFromUserName(user.getUserName());
                    sendAllUserMessage(msg);

                    listModel.removeElement(user.getUserName());
                    historyRecordArea.append(formatMessage("用户 " + user.getUserName() + "下线了！"));
                }
            }
            sc.close();
        }
    }

    // 消息记录显示模板
    public String formatMessage(String connect) {
        return String.format(DateUtils.getCurrentDate(new Date()) + SEPARATOR + "%s\n", connect);
    }

    // 主方法
    public static void main(String[] args) {
        new ChatServer();
    }
}
