package deformablemesh.geometry;

import deformablemesh.MeshImageStack;
import deformablemesh.meshview.DataObject;
import deformablemesh.meshview.FurrowPlaneDataObject;
import deformablemesh.meshview.MeshFrame3D;
import deformablemesh.meshview.SphereDataObject;
import deformablemesh.meshview.TexturedPlaneDataObject;
import deformablemesh.meshview.VectorField;
import deformablemesh.ringdetection.FurrowTransformer;
import deformablemesh.util.Vector3DOps;
import org.scijava.vecmath.Point3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Represents a 2d infinite plane with a center of mass, and a normal.
 * The coordinates are in normalized space.
 *
 * User: msmith
 * Date: 8/5/13
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class Furrow3D implements Interceptable{
    public double[] cm;
    public double[] normal;
    public double[] up;

    //double[] line;
    TexturedPlaneDataObject slice;
    FurrowPlaneDataObject object2;
    boolean showPlane = false;
    /**
     * Creates a 3D furrow based on the center and direction.
     *
     * @param center
     * @param direction
     */
    public Furrow3D(double[] center, double[] direction){
        cm = Arrays.copyOf(center, 3);
        normal = Arrays.copyOf(direction, 3);
    }

    public void create3DObject(){
        showPlane = true;
        slice = null;

        object2 = new FurrowPlaneDataObject(cm, normal);

    }

    public void createTexturedPlane3DObject(MeshImageStack mis){
        showPlane = false;
        object2 = null;
        DeformableMesh3D texturedPlaneGeometry = BinaryMeshGenerator.getQuad(
                new double[]{0,0,0},
                new double[]{1, 0, 0},
                new double[]{0, 1, 0}
        );
        slice = new TexturedPlaneDataObject(texturedPlaneGeometry, mis);

    }

    public DataObject getDataObject(){
        if(showPlane){
            return object2;
        } else{
            return slice;
        }
    }

    public List<List<Triangle3D>> splitMesh(List<Triangle3D> triangles){
        ArrayList<List<Triangle3D>> split = new ArrayList<List<Triangle3D>>();
        ArrayList<Triangle3D> forwared = new ArrayList<Triangle3D>();
        ArrayList<Triangle3D> backwards = new ArrayList<Triangle3D>();
        split.add(forwared);
        split.add(backwards);

        for(Triangle3D triangle: triangles){

            triangle.update();
            double[] tc = triangle.center;
            double[] direction = new double[]{
                    tc[0] - cm[0],
                    tc[1] - cm[1],
                    tc[2] - cm[2]
            };

            double d = Vector3DOps.dot(direction, normal);
            if(d>0){
                forwared.add(triangle);
            } else{
                backwards.add(triangle);
            }

        }


        return split;
    }

    public List<List<Node3D>> splitNodes(List<Node3D> nodes){

        ArrayList<List<Node3D>> split = new ArrayList<>();
        ArrayList<Node3D> left = new ArrayList<>();
        ArrayList<Node3D> right = new ArrayList<>();
        split.add(left);
        split.add(right);

        for(Node3D node: nodes){


            double[] tc = node.getCoordinates();
            double[] direction = new double[]{
                    tc[0] - cm[0],
                    tc[1] - cm[1],
                    tc[2] - cm[2]
            };

            double d = Vector3DOps.dot(direction, normal);
            if(d>0){
                left.add(node);
            } else{
                right.add(node);
            }

        }


        return split;

    }

    public List<Connection3D> getIntersectionConnections(List<Connection3D> connections){
        ArrayList<Connection3D> intersections = new ArrayList<>();
        for(Connection3D connection: connections){
            double[] a = connection.A.getCoordinates();
            double[] b = connection.B.getCoordinates();

            double[] bma = Vector3DOps.difference(b,a);

            double con_proj = Vector3DOps.dot(bma,normal);
            if(Math.abs(con_proj)<1e-10){
                //connection is parallel to the plane of interest.
                continue;
            }

            double[] amc = Vector3DOps.difference(a,cm);
            double amc_proj = Vector3DOps.dot(amc,normal);

            double t = -amc_proj/con_proj;

            if(t>=0&&t<=1){


                intersections.add(connection);

            }

        }
        return intersections;
    }

    public List<double[]> getIntersections(List<Connection3D> connections){

        ArrayList<double[]> intersections = new ArrayList<double[]>();
        for(Connection3D connection: connections){
            double[] a = connection.A.getCoordinates();
            double[] b = connection.B.getCoordinates();

            double[] bma = Vector3DOps.difference(b,a);

            double con_proj = Vector3DOps.dot(bma,normal);
            if(Math.abs(con_proj)<1e-10){
                //connection is parallel to the plane of interest.
                continue;
            }

            double[] amc = Vector3DOps.difference(a,cm);
            double amc_proj = Vector3DOps.dot(amc,normal);

            double t = -amc_proj/con_proj;

            if(t>=0&&t<=1){

                double[] p = new double[]{
                        bma[0]*t + a[0],
                        bma[1]*t + a[1],
                        bma[2]*t + a[2]
                };
                intersections.add(p);

            }

        }
        return intersections;
    }


    public double calculateRadius(List<Connection3D> connections){
        return averageRadius(getIntersections(connections));
    }

    /**
     * Returns the average value of the distance a collection of points is from the
     * center of this furrow3D.
     *
     * @param intersections 3D points, presumably will be intersections.
     * @return
     */
    public double averageRadius(List<double[]> intersections){
        double sum = 0;
        int count = 0;

        for(double[] p: intersections){
            double d = 0;
            for(int i = 0; i<3; i++){
                double v = p[i] - cm[i];
                d += v*v;
            }
            sum += Math.sqrt(d);
            count++;
        }

        return count>0?sum/count:0;
    }

    public double[] minimumRadiusLocation(List<double[]> intersections){

        double[] psum = new double[3];

        int count = 0;

        for(double[] p: intersections){
            for(int i = 0; i<3; i++){
                double v = p[i] - cm[i];
                psum[i] += v;
            }
            count++;
        }

        if(count>0){
            for(int i = 0; i<3; i++){
                psum[i] = psum[i]/count;
            }
        }
        return psum;

    }

    public double distanceTo(double[] loc){
        double[] r = Vector3DOps.difference(loc, cm);
        double m = Vector3DOps.dot(r, normal);
        return m>0?m:-m;
    }

    public static void main(String[] args){

        double[] positions = new double[] {
                -0.5, 0.25, 0.25,
                0.5, 0.125, 0.125,
                0.5, -0.125, 0.125,
                -0.5, -0.25, 0.25,
                -0.5, 0.25, -0.25,
                0.5, 0.125, -0.125,
                0.5, -0.125, -0.125,
                -0.5, -0.25, -0.25

        };
        int[] connection_indices = new int[]{
                0,1, 1,2, 2,3, 3,0, 0,2,
                4,5, 5,6, 6,7, 7,4, 7,5,
                4,0, 5,1, 6,2, 7,3, 4,1,
                5,2, 6,3, 7,0
        };

        int[] trianlge_indices = new int[]{
                0,2,1, 0,3,2, 4,5,7, 7,5,6,
                0,1,4, 4,1,5, 5,1,2, 5,2,6,
                2,3,6, 6,3,7, 7,0,4, 7,3,0
        };

        DeformableMesh3D mesh = new DeformableMesh3D(positions, connection_indices, trianlge_indices);
        mesh.create3DObject();
        MeshFrame3D frame = new MeshFrame3D();
        frame.showFrame(true);
        frame.addDataObject(mesh.data_object);

        SphereDataObject spherez = new SphereDataObject(new double[]{0, 0, 1}, 0.1);
        frame.addDataObject(spherez);

        double theta = 0;
        Furrow3D furrow = null;
        List<SphereDataObject> spheres = new ArrayList<>();

        while(true){
            theta += 0.01;
            double x = 0.5*Math.sin(theta);
            double y = 0.5*Math.cos(theta);

            if(furrow!=null){
                frame.removeDataObject(furrow.getDataObject());
            }
            for(SphereDataObject s: spheres){
                frame.removeDataObject(s);
            }
            spheres.clear();

            furrow = new Furrow3D(new double[]{0, 0, 0}, new double[]{-1.0, 0, 0});
            furrow.create3DObject();
            frame.addDataObject(furrow.getDataObject());


            List<double[]> intersections = furrow.getIntersections(mesh.connections);
            for(double[] intersect: intersections){

                SphereDataObject i = new SphereDataObject(intersect, 0.02);
                frame.addDataObject(i);
                spheres.add(i);
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }



    }

    public void move(double[] displacement) {
        cm[0] += displacement[0];
        cm[1] += displacement[1];
        cm[2] += displacement[2];

        updateGeometry();

    }

    private void updateSliceGeometry() {

        if(showPlane){
            return;
        }

        if(slice==null){
            return;
        }

        MeshImageStack mis = slice.getMeshImageStack();

        int w = mis.getWidthPx();
        int h = mis.getHeightPx();
        FurrowTransformer ft = new FurrowTransformer(this, mis);
        double[] p0 = ft.getVolumeCoordinates(new double[]{0, 0});
        double[] p1 = ft.getVolumeCoordinates(new double[]{0, h});
        double[] p2 = ft.getVolumeCoordinates(new double[]{w, h});
        double[] p3 = ft.getVolumeCoordinates(new double[]{w, 0});
        double[] res = {
                p0[0], p0[1], p0[2],
                p1[0], p1[1], p1[2],
                p2[0], p2[1], p2[2],
                p3[0], p3[1], p3[2]
        };
        slice.updateGeometry(res);
    }


    public void rotateNormalZ(double theta){
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        normal[0] = c*normal[0] - s*normal[1];
        normal[1] = s*normal[0] + c*normal[1];

        updateGeometry();
    }

    public void rotateNormalY(double theta){
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        normal[0] = c*normal[0] + s*normal[2];
        normal[2] = -s*normal[0] + c*normal[2];

        updateGeometry();
    }


    public void moveTo(double[] original) {

        cm[0] = original[0];
        cm[1] = original[1];
        cm[2] = original[2];

        updateGeometry();

    }
    public void updateGeometry(){
        if(showPlane){
            if(object2!=null){
                object2.updatePosition(cm,normal);
            }
        } else{
            updateSliceGeometry();
        }
    }
    public void setDirection(double[] dir) {
        normal[0]=dir[0];
        normal[1]=dir[1];
        normal[2]=dir[2];

        updateGeometry();
    }

    /**
     * Gets the signed distance to the point, if the value is greater than 0 then the point is infront of the plane,
     * if it is negative then the point is behind the plane.
     *
     * @param a node to check
     * @return the normal distance to the plane
     */
    public double getDistance(Node3D a) {
        double[] pt = a.getCoordinates();
        double[] r = new double[]{
                pt[0] - cm[0],
                pt[1] - cm[1],
                pt[2] - cm[2]
        };

        double d = Vector3DOps.dot(r, normal);
        //d = d>0?d:-d;

        return d;
    }

    @Override
    public List<Intersection> getIntersections(double[] origin, double[] direction) {
        double[] r = Vector3DOps.difference(this.cm, origin);
        double toPlane = Vector3DOps.dot(r, normal);

        double dot = Vector3DOps.dot(direction, normal);
        double t = dot<0 ? 1 + dot : 1 - dot;
        if(t < 1e-6 ){
            //parallel to normal.
            return new ArrayList<>();
        }

        double k = toPlane/dot;

        double[] p = Vector3DOps.add(origin, direction, k);

        return Arrays.asList( new Intersection(p, normal));
    }
    @Override
    public boolean contains(double[] pt){
        double[] r = Vector3DOps.difference(this.cm, pt);
        double toPlane = Vector3DOps.dot(r, normal);
        return false;
        //return toPlane>=0;


    }

    public double[] getClosestPointOnPlane(double[] pt) {

        double[] ri = Vector3DOps.difference(pt, cm);
        double h = Vector3DOps.dot(ri, normal);
        //ri - h*normal
        return Vector3DOps.add(pt, normal, -h);

    }

    public void removeDataObject() {
        slice = null;
    }
}
