import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {

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
    }

    public void showGraphics(Double[][] graphicsData) {
        this.graphicsData = graphicsData;
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
        minY = graphicsData[0][1];
        maxY = minY;

// Найти минимальное и максимальное значение функции
        for (int i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < minY) {
                minY = graphicsData[i][1];
            }
            if (graphicsData[i][1] > maxY) {
                maxY = graphicsData[i][1];
            }
        }

// Шаг 4 - Определить (исходя из размеров окна) масштабы по осям X и Y - сколько пикселов приходится на единицу длины по X и по Y
        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);

// Шаг 5 - выбор масштаба (минимальный)
        scale = Math.min(scaleX, scaleY);

// Шаг 6 - корректировка границ отображаемой области согласно выбранному масштабу
        if (scale == scaleX) {
/* Если за основу был взят масштаб по оси X, значит по оси Y
делений меньше,
* т.е. подлежащий визуализации диапазон по Y будет меньше
высоты окна.
* Значит необходимо добавить делений, сделаем это так:
* 1) Вычислим, сколько делений влезет по Y при выбранном масштабе - getSize().getHeight()/scale
* 2) Вычтем из этого сколько делений требовалось изначально
* 3) Набросим по половине недостающего расстояния на maxY и
minY
*/
            double yIncrement = (getSize().getHeight() / scale - (maxY - minY)) / 2;
            maxY += yIncrement;
            minY -= yIncrement;
        }

        if (scale == scaleY) {
// Если за основу был взят масштаб по оси Y, действовать по аналогии
            double xIncrement = (getSize().getWidth() / scale - (maxX - minX)) / 2;
            maxX += xIncrement;
            minX -= xIncrement;
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

// Шаг 9 - Восстановить старые настройки холста
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    protected void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(graphicsStroke);       // Выбрать линию для рисования графика
        canvas.setColor(Color.RED);     // Выбрать цвет линии

        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
// Преобразовать значения (x,y) в точку на экране point
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
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
        System.out.println(graphicsData.length);

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

        Double difference =maxY - minY;

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
        return new Point2D.Double(deltaX * scale, deltaY * scale);
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
}
