package com.rc.forms;

import com.rc.app.Launcher;
import com.rc.app.ShadowBorder;
import com.rc.components.*;
import com.rc.db.model.ContactsUser;
import com.rc.db.service.ContactsUserService;
import com.rc.utils.FontUtil;
import com.rc.utils.OSUtil;
import com.rc.websocket.WebSocketClient;
import com.sun.awt.AWTUtilities;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.rc.app.Launcher.roomService;

/**
 * Created by song on 07/06/2017.
 */
public class CreateGroupDialog extends JDialog
{
    private static CreateGroupDialog context;
    private JPanel editorPanel;
    private RCTextField groupNameTextField;
    private JCheckBox privateCheckBox;

    private SelectUserPanel selectUserPanel;
    private JPanel buttonPanel;
    private JButton cancelButton;
    private JButton okButton;
    private List<String> userList = new ArrayList<>();

    private ContactsUserService contactsUserService = Launcher.contactsUserService;


    public static final int DIALOG_WIDTH = 580;
    public static final int DIALOG_HEIGHT = 500;


    public CreateGroupDialog(Frame owner, boolean modal)
    {
        super(owner, modal);
        context = this;

        initComponents();
        initData();

        initView();
        setListeners();
    }

    private void initData()
    {
        List<ContactsUser> contactsUsers = contactsUserService.findAll();
        for (ContactsUser con : contactsUsers)
        {
            userList.add(con.getName());
        }

        selectUserPanel = new SelectUserPanel(userList);
    }


    private void initComponents()
    {
        int posX = MainFrame.getContext().getX();
        int posY = MainFrame.getContext().getY();

        posX = posX + (MainFrame.getContext().currentWindowWidth - DIALOG_WIDTH) / 2;
        posY = posY + (MainFrame.getContext().currentWindowHeight - DIALOG_HEIGHT) / 2;
        setBounds(posX, posY, DIALOG_WIDTH, DIALOG_HEIGHT);
        setUndecorated(true);

        getRootPane().setBorder(new LineBorder(Colors.LIGHT_GRAY));

        if (OSUtil.getOsType() != OSUtil.Mac_OS)
        {
            // 边框阴影，但是会导致字体失真
            AWTUtilities.setWindowOpaque(this, false);
            //getRootPane().setOpaque(false);
            getRootPane().setBorder(ShadowBorder.newInstance());
        }

        // 输入面板
        editorPanel = new JPanel();
        groupNameTextField = new RCTextField();
        groupNameTextField.setPlaceholder("群聊名称");
        groupNameTextField.setPreferredSize(new Dimension(DIALOG_WIDTH / 2, 35));
        groupNameTextField.setFont(FontUtil.getDefaultFont(14));
        groupNameTextField.setForeground(Colors.FONT_BLACK);
        groupNameTextField.setMargin(new Insets(0, 15, 0, 0));

        privateCheckBox = new JCheckBox("私有");
        privateCheckBox.setFont(FontUtil.getDefaultFont(14));
        privateCheckBox.setToolTipText("私有群组对外不可见，聊天内容无法被非群成员浏览，只有创建者才有权限添加成员，建议勾选此项");
        privateCheckBox.setSelected(true);


        // 按钮组
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        cancelButton = new RCButton("取消");
        cancelButton.setForeground(Colors.FONT_BLACK);

        okButton = new RCButton("创建", Colors.MAIN_COLOR, Colors.MAIN_COLOR_DARKER, Colors.MAIN_COLOR_DARKER);
        okButton.setBackground(Colors.PROGRESS_BAR_START);
    }


    private void initView()
    {
        editorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        editorPanel.add(groupNameTextField);
        editorPanel.add(privateCheckBox);

        buttonPanel.add(cancelButton, new GBC(0, 0).setWeight(1, 1).setInsets(15, 0, 0, 0));
        buttonPanel.add(okButton, new GBC(1, 0).setWeight(1, 1));


        add(editorPanel, BorderLayout.NORTH);
        add(selectUserPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setListeners()
    {
        cancelButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                setVisible(false);

                super.mouseClicked(e);
            }
        });

        okButton.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                String roomName = groupNameTextField.getText();
                if (roomName == null || roomName.isEmpty())
                {
                    JOptionPane.showMessageDialog(null, "请输入群聊名称", "请输入群聊名称", JOptionPane.WARNING_MESSAGE);
                    groupNameTextField.requestFocus();
                    return;
                }

                checkRoomExists(roomName);

                System.out.println(selectUserPanel.getSelectedUser());
                super.mouseClicked(e);
            }
        });
    }

    private void checkRoomExists(String name)
    {
        if (roomService.findByName(name) != null)
        {
            showRoomExistMessage(name);
        }
        else
        {

            createChannelOrGroup(name, privateCheckBox.isSelected(), selectUserPanel.getSelectedUser().toArray(new String[]{}));

            /*HttpGetTask task = new HttpGetTask();
            task.setListener(new HttpResponseListener()
            {
                @Override
                public void onResult(Object ret)
                {
                    System.out.println(ret);
                }
            });

            CurrentUser currentUser = currentUserService.findAll().get(0);
            task.addHeader("X-Auth-Token", currentUser.getAuthToken());
            task.addHeader("X-User-Id", currentUser.getUserId());
            task.execute(Launcher.HOSTNAME + "/api/v1/channels.list?count=" + Integer.MAX_VALUE);*/
        }
    }

    /**
     * 创建Channel或Group
     *
     * @param name
     * @param privateGroup
     * @param usernames
     */
    private void createChannelOrGroup(String name, boolean privateGroup, String[] usernames)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < usernames.length; i++)
        {
            sb.append("\"" + usernames[i] + "\"");
            if (i < usernames.length - 1)
            {
                sb.append(",");
            }
        }
        sb.append("]");

        WebSocketClient.getContext().createChannelOrGroup(name, sb.toString(), privateGroup);


       /* Intent intent = new Intent();
        intent.setAction(WebSocketService.ACTIVITY_TO_WEBSOCKET_ACTION);
        intent.putExtra("event", WebSocketService.EVENT_CREATE_CHANNEL_OR_GROUP);
        intent.putExtra("name", name);
        intent.putExtra("members", sb.toString());
        intent.putExtra("private", privateGroup);
        sendBroadcast(intent);*/
    }

    public static CreateGroupDialog getContext()
    {
        return context;
    }

    public void showRoomExistMessage(String roomName)
    {
        JOptionPane.showMessageDialog(null, "群组\"" + roomName + "\"已存在", "群组已存在", JOptionPane.WARNING_MESSAGE);
        groupNameTextField.setText("");
        groupNameTextField.requestFocus();
    }

}
