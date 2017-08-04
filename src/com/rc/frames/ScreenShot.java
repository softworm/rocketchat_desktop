package com.rc.frames;

import com.rc.components.Colors;
import com.rc.panels.ChatPanel;
import com.rc.utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScreenShot extends JFrame
{
    public static boolean visible = false;
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    private BufferedImage image = null;
    private BufferedImage tempImage = null;
    private BufferedImage saveImage = null;
    private JDialog controlDialog = new JDialog();
    private int selectedWidth;
    private int selectedHeight;
    private int drawX;
    private int drawY;
    private boolean mouseDragged = false;
    private int maxWidth;
    private int maxHeight;
    boolean isShown = false;

    private static final int OUTSIDE_SELECTED = -1;
    private static final int IN_SELECTED_AREA = 0;
    private static final int LEFT_TOP = 1;
    private static final int LEFT_BOTTOM = 2;
    private static final int RIGHT_TOP = 3;
    private static final int RIGHT_BOTTOM = 4;

    private Cursor crossCursor;
    private Cursor moveCursor;
    private Cursor NWresizeCursor;
    private Cursor SWresizeCursor;
    private Cursor NEresizeCursor;
    private Cursor SEresizeCursor;


    private int mouseDownArea = OUTSIDE_SELECTED;

    private java.util.List<WindowUtil.WindowInfo> windowInfoList = new ArrayList<>();
    private boolean mouseDown = false;
    private boolean hitWindow;
    private WindowUtil.WindowInfo activeWindow = null;

    // 绘制标记矩形模式
    private boolean drawSignRectangleMode = false;

    // 绘制箭头模式
    private boolean drawArrowMode = false;

    private int signRectangleX;
    private int signRectangleY;
    private int signRectangleWidth;
    private int signRectangleHeight;
    private boolean selectedHitWindow = false;


    public ScreenShot()
    {
        setUndecorated(true);
        setBackground(Colors.DARK);

        setOpacity(0); //初始时设置窗口为透明，防止窗口闪烁

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        crossCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
        moveCursor = new Cursor(Cursor.MOVE_CURSOR);
        NWresizeCursor = new Cursor(Cursor.NW_RESIZE_CURSOR);
        SWresizeCursor = new Cursor(Cursor.SW_RESIZE_CURSOR);
        NEresizeCursor = new Cursor(Cursor.NE_RESIZE_CURSOR);
        SEresizeCursor = new Cursor(Cursor.SE_RESIZE_CURSOR);

        setCursor(crossCursor);

        screenShot();
        initControlDialog();
        setListeners();

        if (OSUtil.getOsType() == OSUtil.Windows)
        {
            getWindowList();

            activeWindow = WindowUtil.getForegroundWindow();
        }
    }

    /**
     * 获取当前桌面的所有窗口位置信息
     */
    private void getWindowList()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        int screenWidth = tk.getScreenSize().width;
        int screenHeight = tk.getScreenSize().height;


        if (OSUtil.getOsType() == OSUtil.Windows)
        {
            for (WindowUtil.WindowInfo info : WindowUtil.listWindow())
            {
                if (info.getRect().right == screenWidth)
                {
                    continue;
                }

                WindowUtil.RECT rect = info.getRect();

                // 保证窗口大小不越界
                int l = rect.left < 0 ? 0 : rect.left;
                int t = rect.top < 0 ? 0 : rect.top;
                int r = rect.right > screenWidth ? screenWidth : rect.right;
                int b = rect.bottom > screenHeight ? screenHeight : rect.bottom;
                rect.setLeft(l);
                rect.setTop(t);
                rect.setRight(r);
                rect.setBottom(b);

                windowInfoList.add(info);
            }
        }
    }

    private void setListeners()
    {
        this.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                mouseDown = true;

                mouseDownArea = getMousePosition(e);
                startX = e.getX();
                startY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (mouseDragged)
                {
                    mouseDragged = false;

                    controlDialog.setBounds(drawX + 8, drawY + selectedHeight + 10, 250, 40);
                    controlDialog.setVisible(true);
                }

                // 绘制标记矩形模式时，当鼠标释放，把绘制好的矩形画到saveImage中
                if (drawSignRectangleMode)
                {
                    Graphics2D g = (Graphics2D) saveImage.getGraphics();
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(3.0F));
                    g.drawRect(signRectangleX - 1 - drawX, signRectangleY - 1 - drawY, signRectangleWidth + 1, signRectangleHeight + 1);
                    g.dispose();
                }
                else if (drawArrowMode)
                {
                    Graphics2D g = (Graphics2D) saveImage.getGraphics();
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(2.0F));
                    _drawArrow(startX - drawX, startY - drawY, endX - drawX, endY - drawY, g);
                    g.dispose();
                }
            }


            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 1)
                {
                    // 鼠标单击时，如果当前区域是命中窗口区域，则选定该区域
                    if (hitWindow)
                    {
                        selectedHitWindow = true;

                        controlDialog.setBounds(drawX + 8, drawY + selectedHeight + 10, 250, 40);
                        controlDialog.setVisible(true);
                    }
                }
                else if (e.getClickCount() >= 2)
                {
                    close();
                    ClipboardUtil.copyImage(saveImage);
                    ChatPanel.getContext().paste();
                }

                super.mouseClicked(e);
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                mouseDragged = true;
                endX = e.getX();
                endY = e.getY();

                int xDistance = e.getX() - startX;
                int yDistance = e.getY() - startY;


                // 绘制标记矩形
                if (drawSignRectangleMode)
                {
                    signRectangleX = Math.min(startX, endX);
                    signRectangleY = Math.min(startY, endY);
                    signRectangleWidth = Math.abs(endX - startX) + 1;
                    signRectangleHeight = Math.abs(endY - startY) + 1;

                    // 保证矩形不会超出选定区域范围
                    signRectangleWidth = signRectangleWidth > selectedWidth ? selectedWidth : signRectangleWidth;
                    signRectangleHeight = signRectangleHeight > selectedHeight ? selectedHeight : signRectangleHeight;
                    drawSignRectangle();
                }

                // 绘制箭头
                else if (drawArrowMode)
                {
                    drawArrow();
                }
                else
                {
                    // 如果鼠标落在选定区域内，则鼠标移动时移动选定区域
                    if (mouseDownArea == IN_SELECTED_AREA)
                    {
                        // 如果当前区域命中一个窗口区域，且已选中该区域，则在鼠标移动时移动区域，否则在鼠标移动时选定区域
                        if (hitWindow && !selectedHitWindow)
                        {
                            drawX = Math.min(startX, endX);
                            drawY = Math.min(startY, endY);
                            selectedWidth = Math.abs(endX - startX) + 1;
                            selectedHeight = Math.abs(endY - startY) + 1;
                        }
                        else
                        {
                            drawX += xDistance;
                            drawY += yDistance;

                            // 保证不会越界
                            drawX = drawX < 0 ? 0 : drawX;
                            drawY = drawY < 0 ? 0 : drawY;
                            drawX = drawX + selectedWidth > maxWidth ? maxWidth - selectedWidth : drawX;
                            drawY = drawY + selectedHeight > maxHeight ? maxHeight - selectedHeight : drawY;

                            startX = e.getX();
                            startY = e.getY();
                        }
                    }
                    // 选定新的区域
                    else if (mouseDownArea == OUTSIDE_SELECTED)
                    {
                        drawX = Math.min(startX, endX);
                        drawY = Math.min(startY, endY);
                        selectedWidth = Math.abs(endX - startX) + 1;
                        selectedHeight = Math.abs(endY - startY) + 1;
                    }
                    // 落在四个角
                    else
                    {
                        //int xDistance = e.getX() - startX;
                        //int yDistance = e.getY() - startY;


                        switch (mouseDownArea)
                        {
                            case LEFT_TOP:
                            {
                                drawX += xDistance;
                                drawY += yDistance;
                                selectedWidth -= xDistance;
                                selectedHeight -= yDistance;

                                break;
                            }
                            case LEFT_BOTTOM:
                            {
                                drawX += xDistance;

                                selectedWidth -= xDistance;
                                selectedHeight += yDistance;

                                break;
                            }
                            case RIGHT_TOP:
                            {
                                drawY += yDistance;

                                selectedWidth += xDistance;
                                selectedHeight -= yDistance;

                                break;
                            }
                            case RIGHT_BOTTOM:
                            {

                                selectedWidth += xDistance;
                                selectedHeight += yDistance;

                                break;
                            }

                        }
                        selectedWidth = selectedWidth < 1 ? 1 : selectedWidth;
                        selectedHeight = selectedHeight < 1 ? 1 : selectedHeight;

                        startX = e.getX();
                        startY = e.getY();
                    }

                    selectedWidth = selectedWidth > maxWidth ? maxWidth : selectedWidth;
                    selectedHeight = selectedHeight > maxHeight ? maxHeight : selectedHeight;

                    drawSelectedRectangle(true);

                    if (controlDialog.isVisible())
                    {
                        controlDialog.setVisible(false);
                    }
                }

            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                // 如果已经按下了鼠标，则不高亮其他窗口
                if (mouseDown)
                {

                    int mousePosition = getMousePosition(e);
                    switch (mousePosition)
                    {
                        case IN_SELECTED_AREA:
                        {
                            if (drawSignRectangleMode || drawArrowMode)
                            {
                                setCursor(Cursor.getDefaultCursor());
                            }
                            else
                            {
                                setCursor(moveCursor);
                            }
                            break;
                        }
                        case LEFT_TOP:
                        {
                            setCursor(NWresizeCursor);
                            break;
                        }
                        case LEFT_BOTTOM:
                        {
                            setCursor(SWresizeCursor);
                            break;
                        }
                        case RIGHT_TOP:
                        {
                            setCursor(NEresizeCursor);
                            break;
                        }
                        case RIGHT_BOTTOM:
                        {
                            setCursor(SEresizeCursor);
                            break;
                        }
                        case OUTSIDE_SELECTED:
                        {
                            setCursor(crossCursor);
                            break;
                        }
                    }
                }
                else
                {
                    WindowUtil.RECT rect = null;
                    hitWindow = false;

                    // 高亮现有窗口
                    for (WindowUtil.WindowInfo info : windowInfoList)
                    {
                        WindowUtil.RECT rectangle = info.getRect();


                        if (e.getX() >= rectangle.getLeft()
                                && e.getY() >= rectangle.getTop()
                                && e.getX() <= rectangle.getRight()
                                && e.getY() <= rectangle.getBottom())
                        {
                            if (info.getHwnd() == activeWindow.getHwnd())
                            {
                                hitWindow = true;
                                rect = rectangle;
                                break;
                            }

                            if (!inActiveWindow(e.getX(), e.getY()))
                            {
                                hitWindow = true;
                                rect = rectangle;
                                break;
                            }
                        }
                    }

                    if (hitWindow)
                    {
                        drawX = (int) rect.getLeft();
                        drawY = (int) rect.getTop();
                        selectedWidth = (int) rect.getRight() - rect.getLeft();
                        selectedHeight = (int) rect.getBottom() - rect.getTop();

                        drawSelectedRectangle(false);
                    }
                    else
                    {
                        ScreenShot.this.getGraphics().drawImage(tempImage,
                                0, 0, ScreenShot.this);
                    }
                }

                super.mouseMoved(e);
            }
        });

        KeyListener keyListener = new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                {
                    close();
                }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    close();
                    ClipboardUtil.copyImage(saveImage);
                    ChatPanel.getContext().paste();
                }
            }
        };

        addKeyListener(keyListener);
        controlDialog.addKeyListener(keyListener);
    }

    private boolean inActiveWindow(int x, int y)
    {
        int ax1 = activeWindow.getRect().left;
        int ay1 = activeWindow.getRect().top;
        int ax2 = activeWindow.getRect().right;
        int ay2 = activeWindow.getRect().bottom;

        return x >= ax1 && x <= ax2 && y >= ay1 && y <= ay2;
    }


    private void drawSelectedRectangle(boolean drawAnchor)
    {
        Image tempImage2 = createImage(ScreenShot.this.getWidth(), ScreenShot.this.getHeight());
        Graphics2D g = (Graphics2D) tempImage2.getGraphics();
        g.drawImage(tempImage, 0, 0, null);
        g.setColor(Color.CYAN);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        // 绘制选定区域矩形
        g.drawRect(drawX - 1, drawY - 1, selectedWidth + 1, selectedHeight + 1);

        if (drawAnchor)
        {
            // 绘制四角锚点
            g.fillRect(drawX - 8, drawY - 8, 8, 8);
            g.fillRect(drawX + selectedWidth, drawY - 8, 8, 8);
            g.fillRect(drawX - 8, drawY + selectedHeight, 8, 8);
            g.fillRect(drawX + selectedWidth, drawY + selectedHeight, 8, 8);
        }

        // 在选定区域左上角 绘制当前选区大小
        String sizeString = selectedWidth + " * " + selectedHeight;
        g.setFont(FontUtil.getDefaultFont());
        FontMetrics fm = getFontMetrics(g.getFont());
        int h = fm.getHeight() + 6;
        int w = fm.stringWidth(sizeString) + 8;
        g.setColor(Colors.DARK);
        g.fillRect(drawX, drawY - h - 6, w, h);
        g.setColor(Colors.FONT_WHITE);
        g.drawString(sizeString, drawX + 4, drawY - h/2);

        saveImage = image.getSubimage(drawX, drawY, selectedWidth, selectedHeight);
        g.drawImage(saveImage, drawX, drawY, null);

        ScreenShot.this.getGraphics().drawImage(tempImage2,
                0, 0, ScreenShot.this);

        g.dispose();
    }

    /**
     * 绘制标记矩形
     */
    private void drawSignRectangle()
    {
        Image tempImage2 = createImage(saveImage.getWidth(), saveImage.getHeight());
        Graphics2D g = (Graphics2D) tempImage2.getGraphics();
        g.drawImage(saveImage, 0, 0, null);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3.0F));

        // 绘制选定区域矩形
        g.drawRect(signRectangleX - 1 - drawX, signRectangleY - 1 - drawY, signRectangleWidth + 1, signRectangleHeight + 1);

        ScreenShot.this.getGraphics().drawImage(tempImage2,
                drawX, drawY, ScreenShot.this);

        g.dispose();
    }

    /**
     * 绘制箭头
     */
    private void drawArrow()
    {
        Image tempImage2 = createImage(saveImage.getWidth(), saveImage.getHeight());
        Graphics2D g = (Graphics2D) tempImage2.getGraphics();
        g.drawImage(saveImage, 0, 0, null);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2.0F));

        _drawArrow(startX - drawX, startY - drawY, endX - drawX, endY - drawY, g);

        ScreenShot.this.getGraphics().drawImage(tempImage2,
                drawX, drawY, ScreenShot.this);

        g.dispose();
    }

    private void _drawArrow(int startX, int startY, int endX, int endY, Graphics2D g2)
    {
        double H = 20;
        double L = 10;
        int x3 = 0;
        int y3 = 0;
        int x4 = 0;
        int y4 = 0;
        double arrowRec = Math.atan(L / H); // 箭头角度
        double arrowLen = Math.sqrt(L * L + H * H); // 箭头的长度
        double[] arr1 = rotateVec(endX - startX, endY - startY, arrowRec, arrowLen);
        double[] arr2 = rotateVec(endX - startX, endY - startY, -arrowRec, arrowLen);
        double x_3 = endX - arr1[0];
        double y_3 = endY - arr1[1];
        double x_4 = endX - arr2[0];
        double y_4 = endY - arr2[1];

        Double X3 = new Double(x_3);
        x3 = X3.intValue();
        Double Y3 = new Double(y_3);
        y3 = Y3.intValue();
        Double X4 = new Double(x_4);
        x4 = X4.intValue();
        Double Y4 = new Double(y_4);
        y4 = Y4.intValue();
        // 画线
        g2.drawLine(startX, startY, endX, endY);
        //
        GeneralPath triangle = new GeneralPath();
        triangle.moveTo(endX, endY);
        triangle.lineTo(x3, y3);
        triangle.lineTo(x4, y4);
        triangle.closePath();
        //实心箭头
        g2.fill(triangle);
    }

    public static double[] rotateVec(int px, int py, double ang, double newLen)
    {
        double valArr[] = new double[2];
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        double d = Math.sqrt(vx * vx + vy * vy);
        vx = vx / d * newLen;
        vy = vy / d * newLen;
        valArr[0] = vx;
        valArr[1] = vy;
        return valArr;
    }


    private int getMousePosition(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();

        if (x >= drawX && x <= drawX + selectedWidth && y >= drawY && y <= drawY + selectedHeight)
        {
            return IN_SELECTED_AREA;
        }
        else if (x >= drawX - 8 && x <= drawX && y >= drawY - 8 && y <= drawY)
        {
            return LEFT_TOP;
        }
        else if (x >= drawX + selectedWidth && x <= drawX + selectedWidth + 8 && y >= drawY - 8 && y <= drawY)
        {
            return RIGHT_TOP;
        }
        else if (x >= drawX - 8 && x <= drawX && y >= drawY + selectedHeight && y <= drawY + selectedHeight + 8)
        {
            return LEFT_BOTTOM;
        }
        else if (x >= drawX + selectedWidth && x <= drawX + selectedWidth + 8 && y >= drawY + selectedHeight && y <= drawY + selectedHeight + 8)
        {
            return RIGHT_BOTTOM;
        }
        else
        {
            return OUTSIDE_SELECTED;
        }
    }

    private void screenShot()
    {
        //获取默认屏幕设备
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screen = environment.getDefaultScreenDevice();

        //获取屏幕尺寸
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds(0, 0, d.width, d.height);

        //获取屏幕截图
        try
        {
            Robot robot = new Robot(screen);
            image = robot.createScreenCapture(new Rectangle(0, 0, d.width, d.height));

            maxWidth = image.getWidth();
            maxHeight = image.getHeight();
        }
        catch (AWTException e)
        {
            e.printStackTrace();
        }

    }

    private void initControlDialog()
    {
        controlDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        controlDialog.setAlwaysOnTop(true);
        controlDialog.setUndecorated(true);
        controlDialog.setOpacity(0.7F);
        controlDialog.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 6));

        JLabel drawRectLabel = new JLabel(IconUtil.getIcon(this, "/image/rect.png"));
        JLabel drawArrowLabel = new JLabel(IconUtil.getIcon(this, "/image/arrow.png"));
        JLabel okLabel = new JLabel(IconUtil.getIcon(this, "/image/ok.png"));
        JLabel cancelLabel = new JLabel(IconUtil.getIcon(this, "/image/cancel.png"));
        JLabel downloadLabel = new JLabel(IconUtil.getIcon(this, "/image/download.png"));
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        drawRectLabel.setHorizontalAlignment(SwingConstants.CENTER);
        drawRectLabel.setCursor(handCursor);
        drawRectLabel.setToolTipText("绘制矩形");

        drawArrowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        drawArrowLabel.setCursor(handCursor);
        drawArrowLabel.setToolTipText("绘制箭头");

        downloadLabel.setHorizontalAlignment(SwingConstants.CENTER);
        downloadLabel.setCursor(handCursor);
        downloadLabel.setToolTipText("保存截图");

        okLabel.setHorizontalAlignment(SwingConstants.CENTER);
        okLabel.setCursor(handCursor);
        okLabel.setToolTipText("确定");

        cancelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cancelLabel.setCursor(handCursor);
        cancelLabel.setToolTipText("取消");


        drawRectLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent e)
            {
                if (!drawSignRectangleMode)
                {
                    drawRectLabel.setIcon(IconUtil.getIcon(this, "/image/rect.png"));
                }

                super.mouseExited(e);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                drawRectLabel.setIcon(IconUtil.getIcon(this, "/image/rect_active.png"));
                super.mouseEntered(e);
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                drawArrowMode = false;
                drawSignRectangleMode = true;

                drawArrowLabel.setIcon(IconUtil.getIcon(this, "/image/arrow.png"));
                super.mouseClicked(e);
            }
        });

        drawArrowLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent e)
            {
                if (!drawArrowMode)
                {
                    drawArrowLabel.setIcon(IconUtil.getIcon(this, "/image/arrow.png"));
                }

                super.mouseExited(e);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                drawArrowLabel.setIcon(IconUtil.getIcon(this, "/image/arrow_active.png"));
                super.mouseEntered(e);
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                drawSignRectangleMode = false;
                drawArrowMode = true;

                drawRectLabel.setIcon(IconUtil.getIcon(this, "/image/rect.png"));

                super.mouseClicked(e);
            }
        });


        downloadLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                close();
                try
                {
                    saveImage();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
                super.mouseClicked(e);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                downloadLabel.setIcon(IconUtil.getIcon(this, "/image/download_active.png"));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                downloadLabel.setIcon(IconUtil.getIcon(this, "/image/download.png"));
                super.mouseExited(e);
            }
        });

        okLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                close();
                ClipboardUtil.copyImage(saveImage);
                ChatPanel.getContext().paste();
                super.mouseClicked(e);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                okLabel.setIcon(IconUtil.getIcon(this, "/image/ok_active.png"));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                okLabel.setIcon(IconUtil.getIcon(this, "/image/ok.png"));
                super.mouseExited(e);
            }
        });
        cancelLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                close();
                super.mouseClicked(e);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                cancelLabel.setIcon(IconUtil.getIcon(this, "/image/cancel_active.png"));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                cancelLabel.setIcon(IconUtil.getIcon(this, "/image/cancel.png"));
                super.mouseExited(e);
            }
        });

        controlDialog.add(drawRectLabel);
        controlDialog.add(drawArrowLabel);
        controlDialog.add(okLabel);
        controlDialog.add(cancelLabel);
        controlDialog.add(downloadLabel);
    }

    @Override
    public void paint(Graphics g)
    {
        RescaleOp ro = new RescaleOp(0.6f, 0, null);
        tempImage = ro.filter(image, null);
        g.drawImage(tempImage, 0, 0, this);

        if (!isShown)
        {
            setOpacity(1);
            isShown = true;
        }
    }

    //保存图像到文件
    public void saveImage() throws IOException
    {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("保存");

        //文件过滤器，用户过滤可选择的文件
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG", "png");
        jfc.setFileFilter(filter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = "Helichat_screen_shot_" + sdf.format(new Date());

        File filePath = FileSystemView.getFileSystemView().getHomeDirectory();
        File defaultFile = new File(filePath + File.separator + filename + ".png");
        jfc.setSelectedFile(defaultFile);

        int flag = jfc.showSaveDialog(this);
        if (flag == JFileChooser.APPROVE_OPTION)
        {
            File file = jfc.getSelectedFile();
            String path = file.getPath();
            if (!(path.endsWith(".png") || path.endsWith("PNG")))
            {
                path += ".png";
            }
            //写入文件
            ImageIO.write(saveImage, "png", new File(path));
        }
    }


    @Override
    public void setVisible(boolean v)
    {
        if (v)
        {
            boolean frameActive = MainFrame.getContext().isActive();

            if (!frameActive)
            {
                setAlwaysOnTop(true);
                toFront();
            }
        }


        visible = v;
        super.setVisible(v);
    }

    private void close()
    {
        controlDialog.setVisible(false);
        setVisible(false);
        dispose();
    }

}
