package graphing.calculator;

import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import graphing.calculator.three.core.Point;
import graphing.calculator.three.view.DelauneyModifier;
import graphing.calculator.three.view.Mesh3DChartPanel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.shape.DrawMode;

public class BasicView extends View {

    private final Mesh3DChartPanel chart;

    public BasicView(String string) {
        super(string);
        chart = new Mesh3DChartPanel();
        chart.setDelauneyModifier(new MyDelauneyModifier());
        chart.setAxisTitleX("X-Axis");
        chart.setAxisTitleY("Y-Axis");
        chart.setAxisTitleZ("Z-Axis");
        chart.addMeshControlPanel();
        showingProperty().addListener((ob, older, newer) -> {
            if (newer) {
                (new Thread(new TrackTask(getPointsForGauss("")))).start();
            }
        });
    }

    private class TrackTask implements Runnable {

        List<Point3D> points;
        private ProgressIndicator pi;
        private boolean first = false;

        public TrackTask(List<Point3D> point) {
            points = point;
        }

        @Override
        public void run() {
                 pi = new ProgressIndicator(-1);
            Platform.runLater(() -> {
                pi.setMaxWidth(100);
                setCenter(pi);
            });
            DoubleProperty pro = new SimpleDoubleProperty();
            pro.addListener((ob, older, newer) -> {
                Platform.runLater(() -> {
                    pi.setProgress(newer.doubleValue());
                });
            });
            Node n = chart.showMeshPanel(points, BasicView.this, pro);
            Platform.runLater(() -> {
                setCenter(n);
            });
            if (!first) {
                first = true;
            }
        }

        private void write(File fa, ObservableList<String> list) {
            try (FileWriter fw = new FileWriter(fa); PrintWriter pw = new PrintWriter(fw)) {
                for (String s : list) {
                    pw.println(s);
                }
            } catch (IOException ex) {
            }
        }

    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        CheckMenuItem box;
        appBar.getMenuItems().add(box= new CheckMenuItem("Interpolate"));
        box.selectedProperty().addListener((ob, older, newer) -> {
            if (newer) {
                chart.setDrawMode(DrawMode.FILL);
            } else {
                chart.setDrawMode(DrawMode.LINE);
            }
        });
        appBar.setTitleText("3D Graphing");
    }

    private List<Point3D> getPointsForGauss(String s) {
        List<Point3D> result = new ArrayList<>();
        for (double x = -3.0; x <= 3.0; x += 0.20) {
            for (double y = -3.0; y <= 3.0; y += 0.20) {
                result.add(new Point3D(x, x*x+y*y, y));
            }
        }
        return result;
    }

    public static class MyDelauneyModifier implements DelauneyModifier {

        @Override
        public Point convertPoint3d4Delauney(Point3D point) {
            return new Point(point.getX(), point.getZ(), point.getY());
        }

        @Override
        public Point3D convertPointFromDelauney(Point coord) {
            return new Point3D(coord.getX(), coord.getZ(), coord.getY());
        }

    }

    
}
