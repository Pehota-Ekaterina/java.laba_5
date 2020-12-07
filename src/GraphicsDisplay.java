import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import javax.swing.JPanel;
import java.text.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EmptyStackException;
import java.util.Stack;

public class GraphicsDisplay extends JPanel {

    class GraphPoint {
        double xd;
        double yd;
        int x;
        int y;
        int n;
    }

    class Zone {
        double MAXY;
        double tmp;
        double MINY;
        double MAXX;
        double MINX;
        boolean use;
    }

    private Zone zone = new Zone();
    private int[][] graphicsDataI;
    private boolean transform = false;
    private boolean zoom = false;
    private boolean selMode = false;
    private boolean dragMode = false;
    private double scaleX;
    private double scaleY;
    private DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance();
    private BasicStroke selStroke;
    private Font captionFont;
    private int mausePX = 0;
    private int mausePY = 0;
    private GraphPoint SMP;
    double xmax;
    private Rectangle2D.Double rect;
    private Stack<Zone> stack = new Stack<Zone>();

    private Double[][] graphicsData;        // Список координат точек для построения графика

    private boolean showAxis = true;           // Флаговые переменные, задающие правила отображения графика
    private boolean showMarkers = true;
    private boolean showLines = true;

    private double minX;          // Границы диапазона пространства, подлежащего отображению
    private double maxX;
    private double minY;
    private double maxY;

    private double scale;         // Используемый масштаб отображения

    private BasicStroke graphicsStroke;      // Различные стили черчения линий
    private BasicStroke axisStroke;
    private BasicStroke markerStroke;
    private BasicStroke lineStroke;

    private Font axisFont;          // Различные шрифты отображения надписей


    public GraphicsDisplay() {
        setBackground(Color.WHITE);
        graphicsStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[]{1, 1, 1, 1, 1, 1, 3, 1, 2, 1, 2, 1}, 0.0f);
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        lineStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        markerStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        axisFont = new Font("Serif", Font.BOLD, 36);

        selStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8, 8}, 0.0f);
        captionFont = new Font("Serif", Font.BOLD, 10);
        MouseMotionHandler mouseMotionHandler = new MouseMotionHandler();
        addMouseMotionListener(mouseMotionHandler);
        addMouseListener(mouseMotionHandler);
        rect = new Rectangle2D.Double();
        zone.use = false;
    }

    public void showGraphics(Double[][] graphicsData) {
        this.graphicsData = graphicsData;
        graphicsDataI = new int[graphicsData.length][2];
        repaint();      // Запросить перерисовку компонента, т.е. неявно вызвать paintComponent()
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
        repaint();
    }

    // Метод отображения всего компонента, содержащего график
    public void paintComponent(Graphics g) {
        //Шаг 1 - Вызвать метод предка для заливки области цветом заднего фона
        super.paintComponent(g);

// Шаг 2 - Если данные графика не загружены - ничего не делать
        if (graphicsData == null || graphicsData.length == 0) return;

// Шаг 3 - Определить минимальное и максимальное значения для координат X и Y
// Это необходимо для определения области пространства, подлежащей отображению
// Еѐ верхний левый угол это (minX, maxY) - правый нижний это (maxX, minY)
        minX = graphicsData[0][0];
        maxX = graphicsData[graphicsData.length - 1][0];

        if (zone.use) {
            minX = zone.MINX;
        }
        if (zone.use) {
            maxX = zone.MAXX;
        }

        minY = graphicsData[0][1];
        maxY = minY;

// Найти минимальное и максимальное значение функции
        for (int i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < minY) {
                minY = graphicsData[i][1];
            }
            if (graphicsData[i][1] > maxY) {
                maxY = graphicsData[i][1];
                xmax = graphicsData[i][1];
            }
        }

        if (zone.use) {
            minY = zone.MINY;
        }
        if (zone.use) {
            maxY = zone.MAXY;
        }

// Шаг 4 - Определить (исходя из размеров окна) масштабы по осям X и Y - сколько пикселов приходится на единицу длины по X и по Y
        scaleX = 1.0 / (maxX - minX);
        scaleY = 1.0 / (maxY - minY);

        if (!transform)
            scaleX *= getSize().getWidth();
        else
            scaleX *= getSize().getHeight();
        if (!transform)
            scaleY *= getSize().getHeight();
        else
            scaleY *= getSize().getWidth();
        if (transform) {
            ((Graphics2D) g).rotate(-Math.PI / 2);
            ((Graphics2D) g).translate(-getHeight(), 0);
        }

// Шаг 5 - выбор масштаба (минимальный)
        scale = Math.min(scaleX, scaleY);

// Шаг 6 - корректировка границ отображаемой области согласно выбранному масштабу
        if (!zoom) {
            if (scale == scaleX) {
                double yIncrement = 0;
                if (!transform) {
                    yIncrement = (getSize().getHeight() / scale - (maxY - minY)) / 2;
                } else {
                    yIncrement = (getSize().getWidth() / scale - (maxY - minY)) / 2;

                }
                maxY += yIncrement;
                minY -= yIncrement;
            }
            if (scale == scaleY) {
                double xIncrement = 0;
                if (!transform) {
                    xIncrement = (getSize().getWidth() / scale - (maxX - minX)) / 2;
                } else {
                    xIncrement = (getSize().getHeight() / scale - (maxX - minX)) / 2;
                }
                maxX += xIncrement;
                minX -= xIncrement;
            }
        }

// Шаг 7 - Сохранить текущие настройки холста
        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();

// Шаг 8 - В нужном порядке вызвать методы отображения элементов графика
        if (showAxis) paintAxis(canvas);
        paintGraphics(canvas);
        if (showLines) paintLines(canvas);
        if (showMarkers) paintMarkers(canvas);
        if (SMP != null)
            paintHint(canvas);
        if (selMode) {
            canvas.setColor(Color.GREEN);               //ПУНКТИРНАЯ ЛИНИЯ ВЫДЕЛЕНИЯ ОБЛАСТИ
            canvas.setStroke(selStroke);
            canvas.draw(rect);
        }

// Шаг 9 - Восстановить старые настройки холста
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    protected void paintHint(Graphics2D canvas) {
        Color oldColor = canvas.getColor();
        canvas.setColor(Color.GREEN);
        StringBuffer label = new StringBuffer();
        label.append("(x = ");
        label.append(formatter.format((SMP.xd)));
        label.append("; y = ");
        label.append(formatter.format((SMP.yd)));
        label.append(")");
        FontRenderContext context = canvas.getFontRenderContext();
        Rectangle2D bounds = captionFont.getStringBounds(label.toString(), context);
        if (!transform) {
            int dy = -10;
            int dx = +7;
            if (SMP.y < bounds.getHeight())
                dy = +13;
            if (getWidth() < bounds.getWidth() + SMP.x + 20)
                dx = -(int) bounds.getWidth() - 15;
            canvas.drawString(label.toString(), SMP.x + dx, SMP.y + dy);
        } else {
            int dy = 10;
            int dx = -7;
            if (SMP.x < 10)
                dx = +13;
            if (SMP.y < bounds.getWidth() + 20)
                dy = -(int) bounds.getWidth() - 15;
            canvas.drawString(label.toString(), getHeight() - SMP.y + dy, SMP.x + dx);
        }
        canvas.setColor(oldColor);
    }

    protected void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(graphicsStroke);       // Выбрать линию для рисования графика
        canvas.setColor(Color.RED);     // Выбрать цвет линии

        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
// Преобразовать значения (x,y) в точку на экране point
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);

            graphicsDataI[i][0] = (int) point.getX();
            graphicsDataI[i][1] = (int) point.getY();
            if (transform) {
                graphicsDataI[i][0] = (int) point.getY();
                graphicsDataI[i][1] = getHeight() - (int) point.getX();
            }

            if (i > 0) {
                graphics.lineTo(point.getX(), point.getY());        // Не первая итерация цикла - вести линию в точку point
            } else {
                graphics.moveTo(point.getX(), point.getY());        // Первая итерация цикла - установить начало пути в точку point
            }
        }
        canvas.draw(graphics);
    }

    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(markerStroke);

        Double cr_sum = 0.0;
        for (Double[] point : graphicsData) {
            cr_sum += point[1];
        }
        cr_sum /= graphicsData.length;

        for (Double[] point : graphicsData) {

            GeneralPath marker = new GeneralPath();

            int d = 11;
            Point2D.Double center = xyToPoint(point[0], point[1]);

            marker.moveTo(center.x + d / 2, center.y - d / 2);
            marker.lineTo(center.x - d / 2, center.y - d / 2);
            marker.lineTo(center.x, center.y + d / 2);
            marker.closePath();

            if (2 * point[1] > cr_sum) {
                canvas.setColor(Color.BLUE);
                canvas.setPaint(Color.BLUE);
            } else {
                canvas.setColor(Color.RED);
                canvas.setPaint(Color.RED);
            }

            canvas.draw(marker); // Начертить контур маркера
            canvas.fill(marker); // Залить внутреннюю область маркера
        }
    }

    protected void paintLines(Graphics2D canvas) {
        canvas.setStroke(lineStroke);
        canvas.setColor(Color.CYAN);

        Double difference = maxY - minY;

        Point2D.Double center10_min = xyToPoint(minX, minY + 0.1 * difference);
        Point2D.Double center10_max = xyToPoint(maxX, minY + 0.1 * difference);
        Point2D.Double from10 = new Point2D.Double(center10_min.x, center10_min.y);
        Point2D.Double to10 = new Point2D.Double(center10_max.x, center10_max.y);
        Line2D.Double line10 = new Line2D.Double(from10, to10);
        canvas.draw(line10);

        Point2D.Double center50_min = xyToPoint(minX, minY + 0.5 * difference);
        Point2D.Double center50_max = xyToPoint(maxX, minY + 0.5 * difference);
        Point2D.Double from50 = new Point2D.Double(center50_min.x, center50_min.y);
        Point2D.Double to50 = new Point2D.Double(center50_max.x, center50_max.y);
        Line2D.Double line50 = new Line2D.Double(from50, to50);
        canvas.draw(line50);

        Point2D.Double center90_min = xyToPoint(minX, minY + 0.9 * difference);
        Point2D.Double center90_max = xyToPoint(maxX, minY + 0.9 * difference);
        Point2D.Double from90 = new Point2D.Double(center90_min.x, center90_min.y);
        Point2D.Double to90 = new Point2D.Double(center90_max.x, center90_max.y);
        Line2D.Double line90 = new Line2D.Double(from90, to90);
        canvas.draw(line90);
    }

    protected void paintAxis(Graphics2D canvas) {
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setPaint(Color.BLACK);
        canvas.setFont(axisFont);

// Создать объект контекста отображения текста - для получения характеристик устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
// Определить, должна ли быть видна ось Y на графике
        if (minX <= 0.0 && maxX >= 0.0) {
// Она должна быть видна, если левая граница показываемой области (minX) <= 0.0,
// а правая (maxX) >= 0.0
// Сама ось - это линия между точками (0, maxY) и (0, minY)
            canvas.draw(new Line2D.Double(xyToPoint(0, maxY), xyToPoint(0, minY)));
// Стрелка оси Y
            GeneralPath arrow = new GeneralPath();
// Установить начальную точку ломаной точно на верхний конец оси Y
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
// Вести левый "скат" стрелки в точку с относительными координатами (5,20)
            arrow.lineTo(arrow.getCurrentPoint().getX() + 5, arrow.getCurrentPoint().getY() + 20);
// Вести нижнюю часть стрелки в точку с относительными координатами (-10, 0)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 10, arrow.getCurrentPoint().getY());
// Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку

// Нарисовать подпись к оси Y
// Определить, сколько места понадобится для надписи "y"
            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
// Вывести надпись в точке с вычисленными координатами
            canvas.drawString("y", (float) labelPos.getX() + 10, (float) (labelPos.getY() - bounds.getY()));
        }

// Определить, должна ли быть видна ось X на графике
        if (minY <= 0.0 && maxY >= 0.0) {
// Она должна быть видна, если верхняя граница показываемой области (maxX) >= 0.0,
// а нижняя (minY) <= 0.0
            canvas.draw(new Line2D.Double(xyToPoint(minX, 0), xyToPoint(maxX, 0)));
// Стрелка оси X
            GeneralPath arrow = new GeneralPath();
// Установить начальную точку ломаной точно на правый конец оси X
            Point2D.Double lineEnd = xyToPoint(maxX, 0);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
// Вести верхний "скат" стрелки в точку с относительными координатами (-20,-5)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 20, arrow.getCurrentPoint().getY() - 5);
// Вести левую часть стрелки в точку с относительными координатами (0, 10)
            arrow.lineTo(arrow.getCurrentPoint().getX(), arrow.getCurrentPoint().getY() + 10);
// Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелки

// Нарисовать подпись к оси X
// Определить, сколько места понадобится для надписи "x"
            Rectangle2D bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = xyToPoint(maxX, 0);
// Вывести надпись в точке с вычисленными координатами
            canvas.drawString("x", (float) (labelPos.getX() - bounds.getWidth() - 10), (float) (labelPos.getY() + bounds.getY()));
        }
    }

    /* Метод-помощник, осуществляющий преобразование координат.
    * Оно необходимо, т.к. верхнему левому углу холста с координатами
    * (0.0, 0.0) соответствует точка графика с координатами (minX, maxY),
    где
    * minX - это самое "левое" значение X, а
    * maxY - самое "верхнее" значение Y.
    */
    protected Point2D.Double xyToPoint(double x, double y) {
// Вычисляем смещение X от самой левой точки (minX)
        double deltaX = x - minX;
// Вычисляем смещение Y от точки верхней точки (maxY)
        double deltaY = maxY - y;

        if (!zoom)
            return new Point2D.Double(deltaX * scale, deltaY * scale);
        else
            return new Point2D.Double(deltaX * scaleX, deltaY * scaleY);
    }

    protected Point2D.Double pointToXY(int x, int y) {
        Point2D.Double p = new Point2D.Double();
        if (!transform) {
            p.x = x / scale + minX;
            int q = (int) xyToPoint(0, 0).y;
            p.y = maxY - maxY * ((double) y / (double) q);
        } else {
            if (!zoom) {
                p.y = -x / scale + (maxY);
                p.x = -y / scale + maxX;
            } else {
                p.y = -x / scaleY + (maxY);
                p.x = -y / scaleX + maxX;
            }
        }
        return p;
    }

    /* Метод-помощник, возвращающий экземпляр класса Point2D.Double
     * смещѐнный по отношению к исходному на deltaX, deltaY
     */
    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
// Инициализировать новый экземпляр точки
        Point2D.Double dest = new Point2D.Double();
// Задать еѐ координаты как координаты существующей точки + заданные смещения
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }

    public class MouseMotionHandler implements MouseMotionListener, MouseListener {
        private double comparePoint(Point p1, Point p2) {
            return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
        }

        private GraphPoint find(int x, int y) {
            GraphPoint smp = new GraphPoint();
            GraphPoint smp2 = new GraphPoint();
            double r, r2 = 1000;
            for (int i = 0; i < graphicsData.length; i++) {
                Point p = new Point();
                p.x = x;
                p.y = y;
                Point p2 = new Point();
                p2.x = graphicsDataI[i][0];
                p2.y = graphicsDataI[i][1];
                r = comparePoint(p, p2);
                if (r < 7.0) {
                    smp.x = graphicsDataI[i][0];
                    smp.y = graphicsDataI[i][1];
                    smp.xd = graphicsData[i][0];
                    smp.yd = graphicsData[i][1];
                    smp.n = i;
                    if (r < r2) {
                        smp2 = smp;
                    }
                    return smp2;
                }
            }
            return null;
        }

        public void mouseMoved(MouseEvent ev) {    // показывает координаты точек
            GraphPoint smp;
            smp = find(ev.getX(), ev.getY());
            // setCursor(Cursor.getPredefinedCursor(0));
            if (smp != null) {
                SMP = smp;
            } else {
                SMP = null;
            }
            repaint();
        }

        public void mouseDragged(MouseEvent e) {
            if (selMode) {
                if (!transform)
                    rect.setFrame(mausePX, mausePY, e.getX() - rect.getX(),
                            e.getY() - rect.getY());
                else {
                    rect.setFrame(-mausePY + getHeight(), mausePX, -e.getY()
                            + mausePY, e.getX() - mausePX);
                }
                repaint();
            }
            if (dragMode) {
                if (!transform) {
                    if (pointToXY(e.getX(), e.getY()).y < maxY && pointToXY(e.getX(), e.getY()).y > minY) {
                        graphicsData[SMP.n][1] = pointToXY(e.getX(), e.getY()).y;
                        SMP.yd = pointToXY(e.getX(), e.getY()).y;
                        SMP.y = e.getY();
                    }
                } else {
                    if (pointToXY(e.getX(), e.getY()).y < maxY && pointToXY(e.getX(), e.getY()).y > minY) {
                        graphicsData[SMP.n][1] = pointToXY(e.getX(), e.getY()).y;
                        SMP.yd = pointToXY(e.getX(), e.getY()).y;
                        SMP.x = e.getX();
                    }
                }
                repaint();
            }
        }

        public void mouseClicked(MouseEvent e) {        //возрат к обычному масштабу
            if (e.getButton() != 3)
                return;

            try {
                zone = stack.pop();
            } catch (EmptyStackException err) {
            }

            if (stack.empty())
                zoom = false;
            repaint();
        }

        public void mouseEntered(MouseEvent arg0) {
        }

        public void mouseExited(MouseEvent arg0) {
        }

        public void mousePressed(MouseEvent e) {                //показывает область выдиления
            selMode = true;
            mausePX = e.getX();
            mausePY = e.getY();
            rect.setFrame(e.getX(), e.getY(), 0, 0);
        }

        public void mouseReleased(MouseEvent e) {             //увеличение масштаба
            rect.setFrame(0, 0, 0, 0);
            if (e.getButton() != 1) {
                repaint();
                return;
            }
            if (selMode) {
                if (!transform) {
                    if (e.getX() <= mausePX || e.getY() <= mausePY)
                        return;
                    int eY = e.getY();
                    int eX = e.getX();
                    if (eY > getHeight())
                        eY = getHeight();
                    if (eX > getWidth())
                        eX = getWidth();
                    double MAXX = pointToXY(eX, 0).x;
                    double MINX = pointToXY(mausePX, 0).x;
                    double MAXY = pointToXY(0, mausePY).y;
                    double MINY = pointToXY(0, eY).y;
                    stack.push(zone);
                    zone = new Zone();
                    zone.use = true;
                    zone.MAXX = MAXX;
                    zone.MINX = MINX;
                    zone.MINY = MINY;
                    zone.MAXY = MAXY;
                    selMode = false;
                    zoom = true;
                } else {
                    if (pointToXY(mausePX, 0).y <= pointToXY(e.getX(), 0).y
                            || pointToXY(0, e.getY()).x <= pointToXY(0, mausePY).x)
                        return;
                    int eY = e.getY();
                    int eX = e.getX();
                    if (eY < 0)
                        eY = 0;
                    if (eX > getWidth())
                        eX = getWidth();
                    stack.push(zone);
                    zone = new Zone();
                    zone.use = true;
                    zone.MAXY = pointToXY(mausePX, 0).y;
                    zone.MAXX = pointToXY(0, eY).x;
                    zone.MINX = pointToXY(0, mausePY).x;
                    zone.MINY = pointToXY(eX, 0).y;
                    selMode = false;
                    zoom = true;
                }

            }
            repaint();
        }
    }
}
