/* SVGPathParser.java */
/* Generated By:JavaCC: Do not edit this line. SVGPathParser.java */
package sandbox.svg.path;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;

/**
 * 
 * http://www.w3.org/TR/SVG11/paths.html#PathDataBNF
 */
public class SVGPathParser implements SVGPathParserConstants {
        private Point2D.Double start=null;
        private Point2D.Double last=null;

        public SVGPathParser(final String s)
                {
                this(new java.io.StringReader(s));
                }

        public static Shape parse(final String s)
                {
                try
                        {
                        return new SVGPathParser(s).path();
                        }
                catch(final ParseException err)
                        {
                        throw new IllegalArgumentException(err);
                        }
                }

        public static void main(String args[])
                throws Exception
                {
                for(String s:args)
                        {
                        parse(s);
                        }
                }

  final public Shape path() throws ParseException {GeneralPath shape=new GeneralPath();
    label_1:
    while (true) {
      moveTo(shape);
      drawtoCommands(shape);
      if (jj_2_1(2)) {
        ;
      } else {
        break label_1;
      }
    }
    jj_consume_token(0);
{if ("" != null) return shape;}
    throw new Error("Missing return statement in function");
}

  final private void moveTo(GeneralPath shape) throws ParseException {boolean relative;Token t;Point2D.Double p;
    t = jj_consume_token(MOVETO);
relative=t.image.equals("m");
    p = coords();
if(last==null || !relative)
                        {
                        last=p;
                        }
                else
                        {
                        last.x+=p.getX();
                        last.y+=p.getY();
                        }
                shape.moveTo(last.getX(),last.getY());
                this.start=this.last;
    if (jj_2_2(2)) {
      lineToSequence(shape,relative);
    } else {
      ;
    }
}

  final public void drawtoCommands(GeneralPath shape) throws ParseException {
    label_2:
    while (true) {
      if (jj_2_3(2)) {
        ;
      } else {
        break label_2;
      }
      drawtoCommand(shape);
    }
}

  final public void drawtoCommand(GeneralPath shape) throws ParseException {
    if (jj_2_4(2)) {
      closePath(shape);
    } else if (jj_2_5(2)) {
      lineTo(shape);
    } else if (jj_2_6(2)) {
      lineH(shape);
    } else if (jj_2_7(2)) {
      lineV(shape);
    } else if (jj_2_8(2)) {
      cubicBezier(shape);
    } else if (jj_2_9(2)) {
      smoothCubicBezier(shape);
    } else if (jj_2_10(2)) {
      quadraticCurve(shape);
    } else if (jj_2_11(2)) {
      smootQuadraticCurve(shape);
    } else if (jj_2_12(2)) {
      ellipticArc(shape);
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
}

  final private void lineTo(GeneralPath shape) throws ParseException {boolean relative;Token t;
    t = jj_consume_token(LINETO);
relative=t.image.equals("l");
    lineToSequence(shape,relative);
}

  final private void lineToSequence(GeneralPath shape,boolean relative) throws ParseException {Point2D.Double p;
    label_3:
    while (true) {
      p = coords();
if(!relative)
                        {
                        this.last=p;
                        }
                else
                        {
                        this.last.x+=p.getX();
                        this.last.y+=p.getY();
                        }
                shape.lineTo(this.last.getX(),this.last.getY());
      if (jj_2_13(2)) {
        ;
      } else {
        break label_3;
      }
    }
}

  final private void lineH(GeneralPath shape) throws ParseException {boolean relative;Token t;
    t = jj_consume_token(LINEH);
relative=t.image.equals("h");
    lineHSequence(shape,relative);
}

  final private void lineHSequence(GeneralPath shape,boolean relative) throws ParseException {double v;
    label_4:
    while (true) {
      v = coordinate();
if(!relative)
                        {
                        this.last.x=v;
                        }
                else
                        {
                        this.last.x+=v;
                        }
                shape.lineTo(this.last.getX(),this.last.getY());
      if (jj_2_14(2)) {
        ;
      } else {
        break label_4;
      }
    }
}

  final private void lineV(GeneralPath shape) throws ParseException {boolean relative;Token t;
    t = jj_consume_token(LINEV);
relative=t.image.equals("v");
    lineVSequence(shape,relative);
}

  final private void lineVSequence(GeneralPath shape,boolean relative) throws ParseException {double v;
    label_5:
    while (true) {
      v = coordinate();
if(!relative)
                        {
                        this.last.y=v;
                        }
                else
                        {
                        this.last.y+=v;
                        }
                shape.lineTo(this.last.getX(),this.last.getY());
      if (jj_2_15(2)) {
        ;
      } else {
        break label_5;
      }
    }
}

  final private void closePath(GeneralPath shape) throws ParseException {
    jj_consume_token(CLOSEPATH);
shape.closePath();
                this.last=this.start;
}

  final private void cubicBezier(GeneralPath shape) throws ParseException {Token t;
        boolean relative;
        Point2D.Double p1,p2,p3;
    t = jj_consume_token(CUBICBEZIER);
relative=t.image.equals("c");
    label_6:
    while (true) {
      p1 = coords();
      p2 = coords();
      p3 = coords();
if(relative)
                        {
                        p1.x+=last.x;
                        p1.y+=last.y;
                        p2.x+=last.x;
                        p2.y+=last.y;
                        p3.x+=last.x;
                        p3.y+=last.y;
                        }
                shape.curveTo(p1.getX(),p1.getY(),p2.getX(),p2.getY(),p3.getX(),p3.getY());
                this.last=p3;
      if (jj_2_16(2)) {
        ;
      } else {
        break label_6;
      }
    }
}

  final private void smoothCubicBezier(GeneralPath shape) throws ParseException {Token t;
        boolean relative;
        Point2D.Double p1,p2;
    t = jj_consume_token(SMOOTHCUBICBEZIER);
relative=t.image.equals("s");
    label_7:
    while (true) {
      p1 = coords();
      p2 = coords();
if(relative)
                        {
                        p1.x+=last.x;
                        p1.y+=last.y;
                        p2.x+=last.x;
                        p2.y+=last.y;
                        }
                //WRONG TODO
                shape.curveTo(last.getX(),last.getY(),p1.getX(),p1.getY(),p2.getX(),p2.getY());
                this.last=p2;
      if (jj_2_17(2)) {
        ;
      } else {
        break label_7;
      }
    }
}

  final private void quadraticCurve(GeneralPath shape) throws ParseException {Token t;
        boolean relative;
        Point2D.Double p1,p2;
    t = jj_consume_token(QUADRATICCURVE);
relative=t.image.equals("q");
    label_8:
    while (true) {
      p1 = coords();
      p2 = coords();
if(relative)
                        {
                        p1.x+=last.x;
                        p1.y+=last.y;
                        p2.x+=last.x;
                        p2.y+=last.y;
                        }
                //WRONG TODO
                shape.quadTo(p1.getX(),p1.getY(),p2.getX(),p2.getY());
                this.last=p2;
      if (jj_2_18(2)) {
        ;
      } else {
        break label_8;
      }
    }
}

  final private void smootQuadraticCurve(GeneralPath shape) throws ParseException {Token t;
        boolean relative;
        Point2D.Double p1;
    t = jj_consume_token(QUADRATICSMOOTH);
relative=t.image.equals("q");
    label_9:
    while (true) {
      p1 = coords();
if(relative)
                        {
                        p1.x+=last.x;
                        p1.y+=last.y;
                        }
                //WRONG TODO
                shape.quadTo(last.getX(),last.getY(),p1.getX(),p1.getY());
                this.last=p1;
      if (jj_2_19(2)) {
        ;
      } else {
        break label_9;
      }
    }
}

  final private void ellipticArc(GeneralPath shape) throws ParseException {double rx,ry;
        double theta;
        int larg_arc_flag;
        int sweep_flag;
        double x,y;
    jj_consume_token(ELLIPTICARC);
    label_10:
    while (true) {
      rx = coordinate();
      ry = coordinate();
      theta = number();
      larg_arc_flag = integer();
      sweep_flag = integer();
      x = coordinate();
      y = coordinate();
/* this function was copied from 
		   ZZ Coder
		   http://stackoverflow.com/questions/1805101/svg-elliptical-arcs-with-java/1805151#1805151
		  */
                        // Ensure radii are valid
            if (rx == 0 || ry == 0) {
                    shape.lineTo(x, y);
                    this.last.x=x;
                    this.last.y=y;
                    {if ("" != null) return;}
            }
            // Get the current (x, y) coordinates of the shape
            Point2D p2d = shape.getCurrentPoint();
            double x0 = p2d.getX();
            double y0 = p2d.getY();
            // Compute the half distance between the current and the final point
            double dx2 = (x0 - x) / 2.0;
            double dy2 = (y0 - y) / 2.0;
            // Convert theta from degrees to radians
            theta =  Math.toRadians(theta % 360);

            //
            // Step 1 : Compute (x1, y1)
            //
            double x1 = (Math.cos(theta) * (double) dx2 + Math.sin(theta)
                            * (double) dy2);
            double y1 = (-Math.sin(theta) * (double) dx2 + Math.cos(theta)
                            * (double) dy2);

            last.x=x1;
            last.y=y1;

            // Ensure radii are large enough
            rx = Math.abs(rx);
            ry = Math.abs(ry);
            double Prx = rx * rx;
            double Pry = ry * ry;
            double Px1 = x1 * x1;
            double Py1 = y1 * y1;
            double d = Px1 / Prx + Py1 / Pry;
            if (d > 1) {
                    rx = Math.abs((Math.sqrt(d) * (double) rx));
                    ry = Math.abs((Math.sqrt(d) * (double) ry));
                    Prx = rx * rx;
                    Pry = ry * ry;
            }

            //
            // Step 2 : Compute (cx1, cy1)
            //
            double sign = (larg_arc_flag == sweep_flag) ? -1d : 1d;
            double coef = (sign * Math
                            .sqrt(((Prx * Pry) - (Prx * Py1) - (Pry * Px1))
                                            / ((Prx * Py1) + (Pry * Px1))));
            double cx1 = coef * ((rx * y1) / ry);
            double cy1 = coef * -((ry * x1) / rx);

            //
            // Step 3 : Compute (cx, cy) from (cx1, cy1)
            //
            double sx2 = (x0 + x) / 2.0f;
            double sy2 = (y0 + y) / 2.0f;
            double cx = sx2
                            +  (Math.cos(theta) * (double) cx1 - Math.sin(theta)
                                            * (double) cy1);
            double cy = sy2
                            + (double) (Math.sin(theta) * (double) cx1 + Math.cos(theta)
                                            * (double) cy1);

            //
            // Step 4 : Compute the angleStart (theta1) and the angleExtent (dtheta)
            //
            double ux = (x1 - cx1) / rx;
            double uy = (y1 - cy1) / ry;
            double vx = (-x1 - cx1) / rx;
            double vy = (-y1 - cy1) / ry;
            double p, n;
            // Compute the angle start
            n = Math.sqrt((ux * ux) + (uy * uy));
            p = ux; // (1 * ux) + (0 * uy)
            sign = (uy < 0) ? -1d : 1d;
            double angleStart = Math.toDegrees(sign * Math.acos(p / n));
            // Compute the angle extent
            n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
            p = ux * vx + uy * vy;
            sign = (ux * vy - uy * vx < 0) ? -1d : 1d;
            double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
            if (sweep_flag!=1 && angleExtent > 0) {
                    angleExtent -= 360f;
            } else if (sweep_flag==1 && angleExtent < 0) {
                    angleExtent += 360f;
            }
            angleExtent %= 360f;
            angleStart %= 360f;

            Arc2D.Double arc = new Arc2D.Double();
            arc.x = cx - rx;
            arc.y = cy - ry;
            arc.width = rx * 2.0f;
            arc.height = ry * 2.0f;
            arc.start = -angleStart;
            arc.extent = -angleExtent;
            shape.append(arc, true);
      if (jj_2_20(2)) {
        ;
      } else {
        break label_10;
      }
    }
}

  final private Point2D.Double coords() throws ParseException {double x;double y;
    x = coordinate();
    y = coordinate();
{if ("" != null) return new Point2D.Double(x,y);}
    throw new Error("Missing return statement in function");
}

  final private double coordinate() throws ParseException {double n;
    n = number();
{if ("" != null) return n;}
    throw new Error("Missing return statement in function");
}

  final private double number() throws ParseException {double f;
    if (jj_2_21(2)) {
      f = integer();
    } else if (jj_2_22(2)) {
      f = floating();
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
{if ("" != null) return f;}
    throw new Error("Missing return statement in function");
}

  final private double floating() throws ParseException {Token t;
    t = jj_consume_token(FLOATING_NUMBER);
{if ("" != null) return Double.parseDouble(t.image);}
    throw new Error("Missing return statement in function");
}

  final private int integer() throws ParseException {int n;int sig=1;
    if (jj_2_25(2)) {
      if (jj_2_23(2)) {
        jj_consume_token(PLUS);
sig=1;
      } else if (jj_2_24(2)) {
        jj_consume_token(MINUS);
sig=-1;
      } else {
        jj_consume_token(-1);
        throw new ParseException();
      }
    } else {
      ;
    }
    n = positiveInteger();
{if ("" != null) return n*sig;}
    throw new Error("Missing return statement in function");
}

  final private int positiveInteger() throws ParseException {Token t;
    t = jj_consume_token(POSITIVE_INTEGER);
{if ("" != null) return Integer.parseInt(t.image);}
    throw new Error("Missing return statement in function");
}

  private boolean jj_2_1(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_1()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_2_2(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_2()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  private boolean jj_2_3(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_3()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(2, xla); }
  }

  private boolean jj_2_4(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_4()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(3, xla); }
  }

  private boolean jj_2_5(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_5()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(4, xla); }
  }

  private boolean jj_2_6(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_6()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(5, xla); }
  }

  private boolean jj_2_7(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_7()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(6, xla); }
  }

  private boolean jj_2_8(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_8()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(7, xla); }
  }

  private boolean jj_2_9(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_9()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(8, xla); }
  }

  private boolean jj_2_10(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_10()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(9, xla); }
  }

  private boolean jj_2_11(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_11()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(10, xla); }
  }

  private boolean jj_2_12(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_12()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(11, xla); }
  }

  private boolean jj_2_13(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_13()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(12, xla); }
  }

  private boolean jj_2_14(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_14()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(13, xla); }
  }

  private boolean jj_2_15(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_15()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(14, xla); }
  }

  private boolean jj_2_16(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_16()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(15, xla); }
  }

  private boolean jj_2_17(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_17()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(16, xla); }
  }

  private boolean jj_2_18(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_18()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(17, xla); }
  }

  private boolean jj_2_19(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_19()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(18, xla); }
  }

  private boolean jj_2_20(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_20()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(19, xla); }
  }

  private boolean jj_2_21(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_21()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(20, xla); }
  }

  private boolean jj_2_22(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_22()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(21, xla); }
  }

  private boolean jj_2_23(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_23()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(22, xla); }
  }

  private boolean jj_2_24(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_24()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(23, xla); }
  }

  private boolean jj_2_25(int xla)
 {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return (!jj_3_25()); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(24, xla); }
  }

  private boolean jj_3_16()
 {
    if (jj_3R_coords_476_9_23()) return true;
    return false;
  }

  private boolean jj_3R_ellipticArc_357_9_22()
 {
    if (jj_scan_token(ELLIPTICARC)) return true;
    Token xsp;
    if (jj_3_20()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_20()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3_13()
 {
    if (jj_3R_coords_476_9_23()) return true;
    return false;
  }

  private boolean jj_3R_lineToSequence_165_9_12()
 {
    Token xsp;
    if (jj_3_13()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_13()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3_19()
 {
    if (jj_3R_coords_476_9_23()) return true;
    return false;
  }

  private boolean jj_3R_lineTo_159_9_15()
 {
    if (jj_scan_token(LINETO)) return true;
    if (jj_3R_lineToSequence_165_9_12()) return true;
    return false;
  }

  private boolean jj_3R_cubicBezier_249_9_18()
 {
    if (jj_scan_token(CUBICBEZIER)) return true;
    Token xsp;
    if (jj_3_16()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_16()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3_12()
 {
    if (jj_3R_ellipticArc_357_9_22()) return true;
    return false;
  }

  private boolean jj_3_11()
 {
    if (jj_3R_smootQuadraticCurve_332_9_21()) return true;
    return false;
  }

  private boolean jj_3_10()
 {
    if (jj_3R_quadraticCurve_306_9_20()) return true;
    return false;
  }

  private boolean jj_3_9()
 {
    if (jj_3R_smoothCubicBezier_279_9_19()) return true;
    return false;
  }

  private boolean jj_3_8()
 {
    if (jj_3R_cubicBezier_249_9_18()) return true;
    return false;
  }

  private boolean jj_3_7()
 {
    if (jj_3R_lineV_209_9_17()) return true;
    return false;
  }

  private boolean jj_3_24()
 {
    if (jj_scan_token(MINUS)) return true;
    return false;
  }

  private boolean jj_3R_smootQuadraticCurve_332_9_21()
 {
    if (jj_scan_token(QUADRATICSMOOTH)) return true;
    Token xsp;
    if (jj_3_19()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_19()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3_6()
 {
    if (jj_3R_lineH_185_9_16()) return true;
    return false;
  }

  private boolean jj_3_5()
 {
    if (jj_3R_lineTo_159_9_15()) return true;
    return false;
  }

  private boolean jj_3R_closePath_235_9_14()
 {
    if (jj_scan_token(CLOSEPATH)) return true;
    return false;
  }

  private boolean jj_3_4()
 {
    if (jj_3R_closePath_235_9_14()) return true;
    return false;
  }

  private boolean jj_3_3()
 {
    if (jj_3R_drawtoCommand_141_5_13()) return true;
    return false;
  }

  private boolean jj_3R_drawtoCommand_141_5_13()
 {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_4()) {
    jj_scanpos = xsp;
    if (jj_3_5()) {
    jj_scanpos = xsp;
    if (jj_3_6()) {
    jj_scanpos = xsp;
    if (jj_3_7()) {
    jj_scanpos = xsp;
    if (jj_3_8()) {
    jj_scanpos = xsp;
    if (jj_3_9()) {
    jj_scanpos = xsp;
    if (jj_3_10()) {
    jj_scanpos = xsp;
    if (jj_3_11()) {
    jj_scanpos = xsp;
    if (jj_3_12()) return true;
    }
    }
    }
    }
    }
    }
    }
    }
    return false;
  }

  private boolean jj_3_18()
 {
    if (jj_3R_coords_476_9_23()) return true;
    return false;
  }

  private boolean jj_3_2()
 {
    if (jj_3R_lineToSequence_165_9_12()) return true;
    return false;
  }

  private boolean jj_3R_positiveInteger_510_9_30()
 {
    if (jj_scan_token(POSITIVE_INTEGER)) return true;
    return false;
  }

  private boolean jj_3_23()
 {
    if (jj_scan_token(PLUS)) return true;
    return false;
  }

  private boolean jj_3_25()
 {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_23()) {
    jj_scanpos = xsp;
    if (jj_3_24()) return true;
    }
    return false;
  }

  private boolean jj_3R_integer_505_9_25()
 {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_25()) jj_scanpos = xsp;
    if (jj_3R_positiveInteger_510_9_30()) return true;
    return false;
  }

  private boolean jj_3_22()
 {
    if (jj_3R_floating_500_9_26()) return true;
    return false;
  }

  private boolean jj_3_15()
 {
    if (jj_3R_coordinate_484_9_24()) return true;
    return false;
  }

  private boolean jj_3R_lineVSequence_215_9_28()
 {
    Token xsp;
    if (jj_3_15()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_15()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3R_floating_500_9_26()
 {
    if (jj_scan_token(FLOATING_NUMBER)) return true;
    return false;
  }

  private boolean jj_3R_quadraticCurve_306_9_20()
 {
    if (jj_scan_token(QUADRATICCURVE)) return true;
    Token xsp;
    if (jj_3_18()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_18()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3R_lineV_209_9_17()
 {
    if (jj_scan_token(LINEV)) return true;
    if (jj_3R_lineVSequence_215_9_28()) return true;
    return false;
  }

  private boolean jj_3R_moveTo_113_9_11()
 {
    if (jj_scan_token(MOVETO)) return true;
    if (jj_3R_coords_476_9_23()) return true;
    return false;
  }

  private boolean jj_3_21()
 {
    if (jj_3R_integer_505_9_25()) return true;
    return false;
  }

  private boolean jj_3R_number_492_9_29()
 {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_21()) {
    jj_scanpos = xsp;
    if (jj_3_22()) return true;
    }
    return false;
  }

  private boolean jj_3_17()
 {
    if (jj_3R_coords_476_9_23()) return true;
    return false;
  }

  private boolean jj_3R_coordinate_484_9_24()
 {
    if (jj_3R_number_492_9_29()) return true;
    return false;
  }

  private boolean jj_3_1()
 {
    if (jj_3R_moveTo_113_9_11()) return true;
    return false;
  }

  private boolean jj_3_14()
 {
    if (jj_3R_coordinate_484_9_24()) return true;
    return false;
  }

  private boolean jj_3R_lineHSequence_191_9_27()
 {
    Token xsp;
    if (jj_3_14()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_14()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3R_coords_476_9_23()
 {
    if (jj_3R_coordinate_484_9_24()) return true;
    if (jj_3R_coordinate_484_9_24()) return true;
    return false;
  }

  private boolean jj_3R_lineH_185_9_16()
 {
    if (jj_scan_token(LINEH)) return true;
    if (jj_3R_lineHSequence_191_9_27()) return true;
    return false;
  }

  private boolean jj_3_20()
 {
    if (jj_3R_coordinate_484_9_24()) return true;
    if (jj_3R_coordinate_484_9_24()) return true;
    return false;
  }

  private boolean jj_3R_smoothCubicBezier_279_9_19()
 {
    if (jj_scan_token(SMOOTHCUBICBEZIER)) return true;
    Token xsp;
    if (jj_3_17()) return true;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_17()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  /** Generated Token Manager. */
  public SVGPathParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[0];
  static private int[] jj_la1_0;
  static {
	   jj_la1_init_0();
	}
	private static void jj_la1_init_0() {
	   jj_la1_0 = new int[] {};
	}
  final private JJCalls[] jj_2_rtns = new JJCalls[25];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public SVGPathParser(java.io.InputStream stream) {
	  this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public SVGPathParser(java.io.InputStream stream, String encoding) {
	 try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	 token_source = new SVGPathParserTokenManager(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
	  ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
	 try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	 token_source.ReInit(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 0; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public SVGPathParser(java.io.Reader stream) {
	 jj_input_stream = new SimpleCharStream(stream, 1, 1);
	 token_source = new SVGPathParserTokenManager(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
	if (jj_input_stream == null) {
	   jj_input_stream = new SimpleCharStream(stream, 1, 1);
	} else {
	   jj_input_stream.ReInit(stream, 1, 1);
	}
	if (token_source == null) {
 token_source = new SVGPathParserTokenManager(jj_input_stream);
	}

	 token_source.ReInit(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public SVGPathParser(SVGPathParserTokenManager tm) {
	 token_source = tm;
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(SVGPathParserTokenManager tm) {
	 token_source = tm;
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
	 Token oldToken;
	 if ((oldToken = token).next != null) token = token.next;
	 else token = token.next = token_source.getNextToken();
	 jj_ntk = -1;
	 if (token.kind == kind) {
	   jj_gen++;
	   if (++jj_gc > 100) {
		 jj_gc = 0;
		 for (int i = 0; i < jj_2_rtns.length; i++) {
		   JJCalls c = jj_2_rtns[i];
		   while (c != null) {
			 if (c.gen < jj_gen) c.first = null;
			 c = c.next;
		   }
		 }
	   }
	   return token;
	 }
	 token = oldToken;
	 jj_kind = kind;
	 throw generateParseException();
  }

  @SuppressWarnings("serial")
  static private final class LookaheadSuccess extends java.lang.Error {
    @Override
    public Throwable fillInStackTrace() {
      return this;
    }
  }
  static private final LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
	 if (jj_scanpos == jj_lastpos) {
	   jj_la--;
	   if (jj_scanpos.next == null) {
		 jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
	   } else {
		 jj_lastpos = jj_scanpos = jj_scanpos.next;
	   }
	 } else {
	   jj_scanpos = jj_scanpos.next;
	 }
	 if (jj_rescan) {
	   int i = 0; Token tok = token;
	   while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
	   if (tok != null) jj_add_error_token(kind, i);
	 }
	 if (jj_scanpos.kind != kind) return true;
	 if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
	 return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
	 if (token.next != null) token = token.next;
	 else token = token.next = token_source.getNextToken();
	 jj_ntk = -1;
	 jj_gen++;
	 return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
	 Token t = token;
	 for (int i = 0; i < index; i++) {
	   if (t.next != null) t = t.next;
	   else t = t.next = token_source.getNextToken();
	 }
	 return t;
  }

  private int jj_ntk_f() {
	 if ((jj_nt=token.next) == null)
	   return (jj_ntk = (token.next=token_source.getNextToken()).kind);
	 else
	   return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
	 if (pos >= 100) {
		return;
	 }

	 if (pos == jj_endpos + 1) {
	   jj_lasttokens[jj_endpos++] = kind;
	 } else if (jj_endpos != 0) {
	   jj_expentry = new int[jj_endpos];

	   for (int i = 0; i < jj_endpos; i++) {
		 jj_expentry[i] = jj_lasttokens[i];
	   }

	   for (int[] oldentry : jj_expentries) {
		 if (oldentry.length == jj_expentry.length) {
		   boolean isMatched = true;

		   for (int i = 0; i < jj_expentry.length; i++) {
			 if (oldentry[i] != jj_expentry[i]) {
			   isMatched = false;
			   break;
			 }

		   }
		   if (isMatched) {
			 jj_expentries.add(jj_expentry);
			 break;
		   }
		 }
	   }

	   if (pos != 0) {
		 jj_lasttokens[(jj_endpos = pos) - 1] = kind;
	   }
	 }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
	 jj_expentries.clear();
	 boolean[] la1tokens = new boolean[25];
	 if (jj_kind >= 0) {
	   la1tokens[jj_kind] = true;
	   jj_kind = -1;
	 }
	 for (int i = 0; i < 0; i++) {
	   if (jj_la1[i] == jj_gen) {
		 for (int j = 0; j < 32; j++) {
		   if ((jj_la1_0[i] & (1<<j)) != 0) {
			 la1tokens[j] = true;
		   }
		 }
	   }
	 }
	 for (int i = 0; i < 25; i++) {
	   if (la1tokens[i]) {
		 jj_expentry = new int[1];
		 jj_expentry[0] = i;
		 jj_expentries.add(jj_expentry);
	   }
	 }
	 jj_endpos = 0;
	 jj_rescan_token();
	 jj_add_error_token(0, 0);
	 int[][] exptokseq = new int[jj_expentries.size()][];
	 for (int i = 0; i < jj_expentries.size(); i++) {
	   exptokseq[i] = jj_expentries.get(i);
	 }
	 return new ParseException(token, exptokseq, tokenImage);
  }

  private boolean trace_enabled;

/** Trace enabled. */
  final public boolean trace_enabled() {
	 return trace_enabled;
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
	 jj_rescan = true;
	 for (int i = 0; i < 25; i++) {
	   try {
		 JJCalls p = jj_2_rtns[i];

		 do {
		   if (p.gen > jj_gen) {
			 jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
			 switch (i) {
			   case 0: jj_3_1(); break;
			   case 1: jj_3_2(); break;
			   case 2: jj_3_3(); break;
			   case 3: jj_3_4(); break;
			   case 4: jj_3_5(); break;
			   case 5: jj_3_6(); break;
			   case 6: jj_3_7(); break;
			   case 7: jj_3_8(); break;
			   case 8: jj_3_9(); break;
			   case 9: jj_3_10(); break;
			   case 10: jj_3_11(); break;
			   case 11: jj_3_12(); break;
			   case 12: jj_3_13(); break;
			   case 13: jj_3_14(); break;
			   case 14: jj_3_15(); break;
			   case 15: jj_3_16(); break;
			   case 16: jj_3_17(); break;
			   case 17: jj_3_18(); break;
			   case 18: jj_3_19(); break;
			   case 19: jj_3_20(); break;
			   case 20: jj_3_21(); break;
			   case 21: jj_3_22(); break;
			   case 22: jj_3_23(); break;
			   case 23: jj_3_24(); break;
			   case 24: jj_3_25(); break;
			 }
		   }
		   p = p.next;
		 } while (p != null);

		 } catch(LookaheadSuccess ls) { }
	 }
	 jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
	 JJCalls p = jj_2_rtns[index];
	 while (p.gen > jj_gen) {
	   if (p.next == null) { p = p.next = new JJCalls(); break; }
	   p = p.next;
	 }

	 p.gen = jj_gen + xla - jj_la; 
	 p.first = token;
	 p.arg = xla;
  }

  static final class JJCalls {
	 int gen;
	 Token first;
	 int arg;
	 JJCalls next;
  }

        }
