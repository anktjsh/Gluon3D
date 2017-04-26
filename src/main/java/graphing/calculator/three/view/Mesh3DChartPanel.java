package graphing.calculator.three.view;

import com.gluonhq.charm.glisten.mvc.View;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import graphing.calculator.three.core.DelaunayTriangulation;
import graphing.calculator.three.core.Point;
import graphing.calculator.three.core.Triangle;
import graphing.calculator.three.ObjectUtils;
import javafx.beans.property.DoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Mesh3DChartPanel
 */
public class Mesh3DChartPanel {

    private Cube cube;
    private AxisOrientation axisOrientation;
    private MeshView meshView;

    private List<Text> titles;
    private List<Text> labelsX;
    private List<Text> labelsY;
    private List<Text> labelsZ;

    // variables for mouse interaction
    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    // size of mesh/box and size of image, which will be used as a texture for the mesh. If the image is bigger, than the texture has more details, but the calculation time increase
    private int size = com.gluonhq.charm.down.Platform.isDesktop() ? 300 : 100;
    private int imageSize = com.gluonhq.charm.down.Platform.isDesktop() ? 300 : 100;
    private double fontSize = com.gluonhq.charm.down.Platform.isDesktop()?14:6;

    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    // bounds for zooming
    private final double MAX_SCALE = 20.0;
    private final double MIN_SCALE = 0.1;

    // ability to rotate a coordinate, because the delauney algorithm doesn't work with all coordinates. 
    private transient DelauneyModifier delauneyModifier = new DefaultDelauneyModifier();
    private DrawMode drawMode = DrawMode.LINE;
    private boolean dynamicWallsEnabled = true;
    private transient Function<Double, String> formatterFunction = this::formatNumber;

    // axis titles
    private String axisTitleX;
    private String axisTitleY;
    private String axisTitleZ;

    public Mesh3DChartPanel() {
    }

    public Node showMeshPanel(List<Point3D> points, View view, DoubleProperty pro) {
        return draw(points, view, pro);
    }

    public void addMeshControlPanel() {
    }

    public void activateDynamicWalls(boolean pDynamicWallsEnabled) {
        this.dynamicWallsEnabled = pDynamicWallsEnabled;
    }

    public void showOrientationCross(boolean visible) {
        if (axisOrientation != null) {
            Platform.runLater(() -> axisOrientation.setVisible(visible));
        }
    }

    public void setLeftWallVisible(boolean visible) {
        if (cube != null && !dynamicWallsEnabled) {
            Platform.runLater(() -> cube.setLeftWallVisible(visible));
        }
    }

    public void setRightWallVisible(boolean visible) {
        if (cube != null && !dynamicWallsEnabled) {
            Platform.runLater(() -> cube.setRightWallVisible(visible));
        }
    }

    public void setTopWallVisible(boolean visible) {
        if (cube != null && !dynamicWallsEnabled) {
            Platform.runLater(() -> cube.setTopWallVisible(visible));
        }
    }

    public void setBottomWallVisible(boolean visible) {
        if (cube != null && !dynamicWallsEnabled) {
            Platform.runLater(() -> cube.setBottomWallVisible(visible));
        }
    }

    public void setFrontWallVisible(boolean visible) {
        if (cube != null && !dynamicWallsEnabled) {
            Platform.runLater(() -> cube.setFrontWallVisible(visible));
        }
    }

    public void setColor(Color c) {
        for (Text n : titles) {
            n.setFill(c);
        }
        for (Text n : labelsX) {
            n.setFill(c);
        }
        for (Text n : labelsY) {
            n.setFill(c);
        }
        for (Text n : labelsZ) {
            n.setFill(c);
        }
        cube.setFill(c);
    }

    public void setBackWallVisible(boolean visible) {
        if (cube != null && !dynamicWallsEnabled) {
            Platform.runLater(() -> cube.setBackWallVisible(visible));
        }
    }

    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
        if (meshView != null) {
            Platform.runLater(() -> meshView.setDrawMode(drawMode));
        }
    }

    public void setDelauneyModifier(DelauneyModifier delauneyModifier) {
        this.delauneyModifier = delauneyModifier;
    }

    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }

    public void setAxisTitleX(String axisTitleX) {
        this.axisTitleX = axisTitleX;
    }

    public void setAxisTitleY(String axisTitleY) {
        this.axisTitleY = axisTitleY;
    }

    public void setAxisTitleZ(String axisTitleZ) {
        this.axisTitleZ = axisTitleZ;
    }

    private void updateControlPanel() {
    }

    private Node draw(List<Point3D> dataPoints, View view, DoubleProperty pro) {
        MeshCalculationComposite calculationObject = MeshCalculationComposite.of(dataPoints, size);

        List<Point> normalizedOldPoints = new ArrayList<>(calculationObject.getNormalizedPoints().size());
        for (Point3D point : calculationObject.getNormalizedPoints()) {
            normalizedOldPoints.add(delauneyModifier.convertPoint3d4Delauney(point));
        }

        List<Triangle> triangulation = new DelaunayTriangulation(normalizedOldPoints).getTriangulation();
        for (Triangle triangle : triangulation) {
            if (!triangle.isHalfplane()) {
                calculationObject.addTriangle3D(Triangle3D.of(
                        delauneyModifier.convertPointFromDelauney(triangle.getA()),
                        delauneyModifier.convertPointFromDelauney(triangle.getB()),
                        delauneyModifier.convertPointFromDelauney(triangle.getC())));
            }
        }

        cube = new Cube(size);
        cube.applyOnAllWalls(w -> w.setMouseTransparent(true));
        createAxisTitles();
        createAxisLegend(calculationObject);
        cube.getChildren().addAll(titles);
        cube.getChildren().addAll(labelsX);
        cube.getChildren().addAll(labelsY);
        cube.getChildren().addAll(labelsZ);

        cube.getTransforms().addAll(rotateX, rotateY);

        axisOrientation = new AxisOrientation(size + 100);
        axisOrientation.setVisible(false);
        axisOrientation.setMouseTransparent(true);
        cube.getChildren().add(axisOrientation);
        TriangleMesh mesh = new TriangleMesh();
        for (Point3D point : calculationObject.getNormalizedPoints()) {
            mesh.getPoints().addAll((float) point.getX(), (float) (-1. * point.getY()), (float) point.getZ());
        }
        for (Point3D point : calculationObject.getNormalizedPoints()) {
            mesh.getTexCoords().addAll((float) (point.getX() / size), (float) (point.getZ() / size));
        }
        List<Point3D> normalizedPoints = calculationObject.getNormalizedPoints();
        for (Triangle3D triangle : calculationObject.getTriangle3DList()) {
            int faceIndex1 = normalizedPoints.indexOf(triangle.getP0());
            int faceIndex2 = normalizedPoints.indexOf(triangle.getP1());
            int faceIndex3 = normalizedPoints.indexOf(triangle.getP2());
            mesh.getFaces().addAll(faceIndex1, faceIndex1, faceIndex2, faceIndex2, faceIndex3, faceIndex3);
        }
        MeshImageBuilder imageBuilder = new MeshImageBuilder(imageSize);
        Image diffuseMap = imageBuilder.createImage(calculationObject, pro);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(diffuseMap);
        material.setSelfIlluminationMap(diffuseMap);
        meshView = new MeshView(mesh);
        meshView.setTranslateX(-0.5 * size);
        meshView.setTranslateY(0.5 * size);
        meshView.setTranslateZ(-0.5 * size);
        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.NONE);
        meshView.setDrawMode(drawMode);
        meshView.setDepthTest(DepthTest.ENABLE);

        cube.getChildren().addAll(meshView);

        StackPane root = new StackPane();
        root.setPickOnBounds(false);
        root.getChildren().add(cube);

//        view.getScene().setCamera(new PerspectiveCamera());
//        view.getScene().getCamera().setNearClip(0.1);
        view.setOnMousePressed(this::mousePressed);
        view.setOnMouseDragged(this::mouseDragged);
        makeZoomable(view, root);
        updateControlPanel();
        return root;
    }

    private void mouseDragged(MouseEvent me) {
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        rotateX.setAngle(rotateX.getAngle() - (mousePosY - mouseOldY));
        rotateY.setAngle(rotateY.getAngle() + (mousePosX - mouseOldX));

        // shift angle so it is between 0 and 360
        double angleX = rotateX.getAngle() % 360;
        double angleY = rotateY.getAngle() % 360;
        angleX = angleX < 0 ? angleX + 360 : angleX;
        angleY = angleY < 0 ? angleY + 360 : angleY;

        boolean isCubeUpsideDown = 90 <= angleX && angleX < 270;

        if (dynamicWallsEnabled) {

            // set walls visible
            cube.setBottomWallVisible(angleX < 180);
            cube.setTopWallVisible(!cube.isBottomWallVisible());
            cube.setFrontWallVisible((90 <= angleY && angleY < 270) ^ isCubeUpsideDown);
            cube.setRightWallVisible((180 <= angleY) ^ isCubeUpsideDown);
            cube.setBackWallVisible(!cube.isFrontWallVisible());
            cube.setLeftWallVisible(!cube.isRightWallVisible());

            // move/translate labels
            final double labelXTranslateY = (angleX < 180 ? 1 : -1) * 0.5 * size;
            final double labelXTranslateZ = ((cube.isFrontWallVisible()) ? 1 : -1) * 0.5 * size;
            titles.get(0).setTranslateY(labelXTranslateY * 1.1);
            titles.get(0).setTranslateZ(labelXTranslateZ * 1.1);
            for (Node label : labelsX) {
                label.setTranslateY(labelXTranslateY);
                label.setTranslateZ(labelXTranslateZ);
            }

            final double labelYTranslateX = (cube.isBackWallVisible() ^ isCubeUpsideDown ? 1 : -1) * 0.5 * size;
            double labelYTranslateZ = (angleY < 180 ? 1 : -1) * 0.5 * size;
            titles.get(1).setTranslateX(labelYTranslateX);
            titles.get(1).setTranslateZ(labelYTranslateZ);
            labelYTranslateZ = !isCubeUpsideDown ? labelYTranslateZ : labelYTranslateZ + (0 * size / 15.0);
            for (Node label : labelsY) {
                label.setTranslateX(labelYTranslateX);
                label.setTranslateZ(labelYTranslateZ);
            }

            double labelZTranslateX = ((cube.isLeftWallVisible()) ? 1 : -1.) * 0.5 * size;
            final double labelZTranslateY = (angleX < 180 ? 1 : -1) * 0.5 * size;
            titles.get(2).setTranslateX(labelZTranslateX * 1.1);
            titles.get(2).setTranslateY(labelZTranslateY * 1.1);
            labelZTranslateX = cube.isLeftWallVisible() ? labelZTranslateX : labelZTranslateX - (size / 15.0);
            for (Node label : labelsZ) {
                label.setTranslateX(labelZTranslateX);
                label.setTranslateY(labelZTranslateY);
            }

        }

        List<Node> labels = new ArrayList<>(33);
        labels.addAll(titles);
        labels.addAll(labelsX);
        labels.addAll(labelsY);
        labels.addAll(labelsZ);

        calculateAndRotatoNodes(labels, 0.0, Math.toRadians(angleX), Math.toRadians(angleY));

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
    }

    private void mousePressed(MouseEvent mouseEvent) {
        mouseOldX = mouseEvent.getSceneX();
        mouseOldY = mouseEvent.getSceneY();
    }

    private void calculateAndRotatoNodes(List<Node> nodes, double alp, double bet, double gam) {
        double A11 = Math.cos(alp) * Math.cos(gam);
        double A12 = Math.cos(bet) * Math.sin(alp) + Math.cos(alp) * Math.sin(bet) * Math.sin(gam);
        double A13 = Math.sin(alp) * Math.sin(bet) - Math.cos(alp) * Math.cos(bet) * Math.sin(gam);
        double A21 = -Math.cos(gam) * Math.sin(alp);
        double A22 = Math.cos(alp) * Math.cos(bet) - Math.sin(alp) * Math.sin(bet) * Math.sin(gam);
        double A23 = Math.cos(alp) * Math.sin(bet) + Math.cos(bet) * Math.sin(alp) * Math.sin(gam);
        double A31 = Math.sin(gam);
        double A32 = -Math.cos(gam) * Math.sin(bet);
        double A33 = Math.cos(bet) * Math.cos(gam);

        double d = Math.acos((A11 + A22 + A33 - 1d) / 2d);
        if (!ObjectUtils.equalsDoublePrecision(d, 0.0)) {
            double den = 2d * Math.sin(d);
            Point3D p = new Point3D((A32 - A23) / den, (A13 - A31) / den, (A21 - A12) / den);
            for (Node node : nodes) {
                node.setRotationAxis(p);
                node.setRotate(Math.toDegrees(d));
            }
        }
    }

    private void createAxisLegend(MeshCalculationComposite calculationObject) {
        labelsX = new ArrayList<>(10);
        labelsY = new ArrayList<>(10);
        labelsZ = new ArrayList<>(10);

        // for x axis
        for (int i = 0; i < 10; i++) {
            double number = calculationObject.getMinX() + (calculationObject.getSizeX() / 10.0) * i;
            Text text = new Text(formatterFunction.apply(number));
            text.setFont(new Font(fontSize));

            text.setTranslateX(-0.5 * size + i * (size / 10));
            text.setTranslateY(0.5 * size);
            text.setTranslateZ(-0.5 * size);
            text.setMouseTransparent(true);

            labelsX.add(text);
        }

        // for y axis
        for (int i = 0; i < 10; i++) {
            double number = calculationObject.getMinY() + (calculationObject.getSizeY() / 10.0) * i;
            Text text = new Text(formatterFunction.apply(number));
            text.setFont(new Font(fontSize));
            text.setTranslateX(0.5 * size);
            text.setTranslateY(0.5 * size - i * (size / 10));
            text.setTranslateZ(0.5 * size);
            text.setMouseTransparent(true);

            labelsY.add(text);
        }

        // for z axis
        for (int i = 0; i < 10; i++) {
            double number = calculationObject.getMinZ() + (calculationObject.getSizeZ() / 10.0) * i;
            Text text = new Text(formatterFunction.apply(number));
            text.setFont(new Font(fontSize));
            text.setTranslateX(0.5 * size);
            text.setTranslateY(0.5 * size);
            text.setTranslateZ(-0.5 * size + i * (size / 10));
            text.setMouseTransparent(true);

            labelsZ.add(text);
        }

    }

    private void createAxisTitles() {
        titles = new ArrayList<>(3);

        Text label = new Text(axisTitleX);
        label.setTranslateX(-0.05 * size);
        label.setTranslateY((0.5 * size) + 15);
        label.setTranslateZ(-0.5 * size - 15);
        label.setFont(new Font(fontSize));
        label.setMouseTransparent(true);
        titles.add(label);

        label = new Text(axisTitleZ);
        label.setTranslateX((0.5 * size) - 30);
        label.setTranslateY(-0.05 * size);
        label.setTranslateZ(0.5 * size - 15);
        label.setMouseTransparent(true);
        label.setFont(new Font(fontSize));
        titles.add(label);

        label = new Text(axisTitleY);
        label.setTranslateX(0.5 * size + 15);
        label.setTranslateY(0.5 * size + 15);
        label.setFont(new Font(fontSize));
        label.setMouseTransparent(true);
        titles.add(label);
    }

    public void setNumberFormatter(Function<Double, String> formatterFunction) {
        this.formatterFunction = formatterFunction;
    }

    private String formatNumber(double number) {
        String result = Double.toString(number);
        int maxLength = 5;
        if (result.length() > maxLength) {
            int pointIndex = result.indexOf(".");

            if (pointIndex > result.length() - 3) {
                result = result.substring(0, pointIndex);
            } else {
                result = result.substring(0, maxLength + 1 < pointIndex ? pointIndex : maxLength + 1);
            }
        }

        while (result.length() > 1 && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }

        if (result.endsWith(".")) {
            result += "0";
        }

        return result;
    }

    public void makeZoomable(View view, Node control4Scaling) {
        if (!com.gluonhq.charm.down.Platform.isDesktop()) {
            control4Scaling.setScaleX(2.5);
            control4Scaling.setScaleY(2.5);
        }
    }

}
