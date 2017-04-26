package graphing.calculator.three.view;

import javafx.geometry.Point3D;
import graphing.calculator.three.core.Point;

/**
 * Conversion between the different Point classes. Sometimes the delauney
 * algorithm only works if we change the axis. In addition these methods can
 * change the axis.
 */
public interface DelauneyModifier {

    public Point convertPoint3d4Delauney(Point3D point);

    public Point3D convertPointFromDelauney(Point coord);

}
