package com.rc.components.message;

import com.rc.components.Colors;
import com.rc.components.MessageImageLabel;
import com.rc.components.RCMenuItemUI;
import com.rc.components.SizeAutoAdjustTextArea;
import com.rc.entity.MessageItem;
import com.rc.forms.ChatPanel;
import com.rc.utils.ClipboardUtil;
import com.rc.utils.ImageCache;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.Map;

/**
 * Created by song on 2017/6/5.
 */
public class MessagePopupMenu extends JPopupMenu
{
    private int messageType;
    private ImageCache imageCache = new ImageCache();

    public MessagePopupMenu()
    {
        initMenuItem();
    }

    private void initMenuItem()
    {
        JMenuItem item1 = new JMenuItem("复制");
        JMenuItem item2 = new JMenuItem("删除");
        JMenuItem item3 = new JMenuItem("转发");

        item1.setUI(new RCMenuItemUI());
        item1.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                switch (messageType)
                {
                    case MessageItem.RIGHT_TEXT:
                    case MessageItem.LEFT_TEXT:
                    {
                        SizeAutoAdjustTextArea textArea = (SizeAutoAdjustTextArea) getInvoker();
                        String text = textArea.getSelectedText() == null ? textArea.getText() : textArea.getSelectedText();
                        if (text != null)
                        {
                            ClipboardUtil.copyString(text);
                        }
                        break;
                    }
                    case (MessageItem.RIGHT_IMAGE):
                    case(MessageItem.LEFT_IMAGE):
                    {
                        MessageImageLabel imageLabel = (MessageImageLabel) getInvoker();
                        Object obj = imageLabel.getTag();
                        if (obj != null)
                        {
                            Map map = (Map) obj;
                            String id = (String) map.get("attachmentId");
                            String url = (String) map.get("url");
                            imageCache.requestOriginalAsynchronously(id, url, new ImageCache.ImageCacheRequestListener()
                            {
                                @Override
                                public void onSuccess(ImageIcon icon, String path)
                                {
                                    if (path != null && !path.isEmpty())
                                    {
                                        ClipboardUtil.copyImage(path);
                                    }
                                    else
                                    {
                                        System.out.println("图片不存在，复制失败");
                                    }
                                }

                                @Override
                                public void onFailed(String why)
                                {
                                    System.out.println("图片不存在，复制失败");
                                }
                            });
                        }
                        break;
                    }
                }

            }
        });


        item2.setUI(new RCMenuItemUI());
        item2.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String messageId = null;
                switch (messageType)
                {
                    case MessageItem.RIGHT_TEXT:
                    case MessageItem.LEFT_TEXT:
                    {
                        SizeAutoAdjustTextArea textArea = (SizeAutoAdjustTextArea) getInvoker();
                        messageId = textArea.getTag().toString();
                        break;
                    }
                    case (MessageItem.RIGHT_IMAGE):
                    case(MessageItem.LEFT_IMAGE):
                    {
                        MessageImageLabel imageLabel = (MessageImageLabel) getInvoker();
                        Object obj = imageLabel.getTag();
                        if (obj != null)
                        {
                            Map map = (Map) obj;
                            messageId = (String) map.get("messageId");
                        }
                        break;
                    }
                }

                if (messageId != null && !messageId.isEmpty())
                {
                    ChatPanel.getContext().deleteMessage(messageId);
                }
            }
        });

        item3.setUI(new RCMenuItemUI());
        item3.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("转发");
            }
        });

        this.add(item1);
        this.add(item2);
       //this.add(item3);

        setBorder(new LineBorder(Colors.SCROLL_BAR_TRACK_LIGHT));
        setBackground(Colors.FONT_WHITE);
    }

    @Override
    public void show(Component invoker, int x, int y)
    {
        throw new RuntimeException("此方法不会弹出菜单，请调用 show(Component invoker, int x, int y, int messageType) ");
        //super.show(invoker, x, y);
    }

    public void show(Component invoker, int x, int y, int messageType)
    {
        this.messageType = messageType;
        super.show(invoker, x, y);
    }
}
