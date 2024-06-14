package sandbox.tools.comicsbuilder.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sandbox.awt.HasBoundingBox;
import sandbox.awt.HasDimension;
import sandbox.xml.minidom.Element;

public interface Layout extends HasDimension {
	public String getName();
	public int getPanelCount();
	public HasBoundingBox getPanel(int idx);

	public static class Pane implements HasBoundingBox {
		 double x;
		 double y;
		 double width;
		 double height;
		
		@Override
		public double getHeight() {
			return height;
			}
		@Override
		public double getWidth() {
			return width;
			}
		@Override
		public double getX() {
			return x;
			}
		@Override
		public double getY() {
			return y;
			}
		}
	
	abstract class AbstractLayout implements Layout {
		Layout delegate;
		AbstractLayout(Layout delegate) {
			this.delegate=delegate;
			}
		@Override
		public double getWidth() {
			return delegate.getWidth();
			}
		@Override
		public double getHeight() {
			return delegate.getHeight();
			}
		@Override
		public String getName() {
			return delegate.getName();
			}
		@Override
		public int getPanelCount() {
			return delegate.getPanelCount();
			}
		}

	class ScaledLayout extends AbstractLayout {
		private double width = 0;
		private double height = 0;
		ScaledLayout(Layout delegate,double width,double height) {
			super(delegate);
			this.width = width;
			this.height = height;
			}
		@Override
		public HasBoundingBox getPanel(int idx) {
			HasBoundingBox bb = delegate.getPanel(idx);
			double dx = delegate.getWidth()/this.getWidth();
			double dy = delegate.getHeight()/this.getHeight();
			final Pane pane= new Pane();
			pane.x = bb.getX() * dx;
			pane.y = bb.getY() * dy;
			pane.width = bb.getWidth() * dx;
			pane.height = bb.getHeight() * dy;
			return pane;
			}	

		@Override
		public double getWidth() {
			return width;
			}
		@Override
		public double getHeight() {
			return height;
			}
		}
	
	class MirrorHLayout extends AbstractLayout {
		MirrorHLayout(Layout delegate) {
			super(delegate);
			}
		
		@Override
		public HasBoundingBox getPanel(int idx) {
			HasBoundingBox bb = delegate.getPanel(idx);
			final Pane pane= new Pane();
			pane.x = bb.getWidth()+this.getWidth()/2 -bb.getX();
			pane.y = bb.getY() ;
			pane.width = bb.getWidth();
			pane.height = bb.getHeight();
			return pane;
			}
		}
	class MirrorVLayout extends AbstractLayout {
		MirrorVLayout(Layout delegate) {
			super(delegate);
			}
		
		@Override
		public HasBoundingBox getPanel(int idx) {
			HasBoundingBox bb = delegate.getPanel(idx);
			final Pane pane= new Pane();
			pane.x = bb.getX();
			pane.y = bb.getY() ;
			pane.width = bb.getWidth();
			pane.height = bb.getHeight();
			return pane;
			}
		}
	
	class LayoutImpl implements Layout {
		private String name;
		private double width = 0;
		private double height = 0;
		private final List<HasBoundingBox> panels = new ArrayList<>();
		
		public String getName() 
			{
			return this.name;
			}
		@Override
		public double getHeight() {
			return width;
			}
		@Override
		public double getWidth() {
			return height;
			}
		public int getPanelCount() {
			return this.panels.size();
			}
		@Override
		public HasBoundingBox getPanel(int idx) {
			return this.panels.get(idx);
			}
		}
	
	
	
	static public List<Layout> parse(final Element root) {
		return root.assertHasLocalName("layouts").
			elements().
			filter(E->E.hasLocalName("layout")).
			map(E->parseLayout(E)).
			filter(E->!E.panels.isEmpty()).
			collect(Collectors.toList());
		}

	
	static  LayoutImpl parseLayout(final Element root) {
		root.assertHasLocalName("layout");
		final LayoutImpl layout = new LayoutImpl();
		layout.name = root.getAttribute("name").get();
		layout.width = root.getDoubleAttribute("width").getAsDouble();
		layout.height = root.getDoubleAttribute("height").getAsDouble();
		layout.panels.addAll( root.elements().
			filter(E->E.hasLocalName("pane")).
			map(E->parsePane(layout,E)).
			collect(Collectors.toList())
			);
		return layout;
		}
	
	static  Pane parsePane(final LayoutImpl layout,final Element root) {
		root.assertHasLocalName("layout");
		final Pane pane = new Pane();
		pane.width = root.getDoubleAttribute("width").getAsDouble();
		pane.height = root.getDoubleAttribute("height").getAsDouble();
		pane.x = root.getDoubleAttribute("x").getAsDouble();
		pane.y = root.getDoubleAttribute("y").getAsDouble();
		return pane;
		}
}
