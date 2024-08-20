
package sandbox.swing.xml;

import sandbox.Logger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.OptionalDouble;
import org.w3c.dom.Element;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes" })
abstract class BaseSwingXmlContext extends AbstractSwingXmlContext {
	private static final Logger LOG = Logger.builder(BaseSwingXmlContext.class).build();

	
	protected class DialogNodeHandler extends
		WindowNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Dialog.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Dialog instance= java.awt.Dialog.class.cast(instance_o);
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"visible");
					if(opt.isPresent()) instance.setVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"background",java.awt.Color.class);
					if(opt.isPresent()) instance.setBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"undecorated");
					if(opt.isPresent()) instance.setUndecorated((boolean)opt.get());
					
				}
			
			
			
				{
				
					/* skip setModalityType : java.awt.Dialog$ModalityType */
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"title");
					if(opt.isPresent()) instance.setTitle(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"resizable");
					if(opt.isPresent()) instance.setResizable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"opacity");
					if(opt.isPresent()) instance.setOpacity((float)opt.getAsDouble());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"shape",java.awt.Shape.class);
					if(opt.isPresent()) instance.setShape(java.awt.Shape.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"modal");
					if(opt.isPresent()) instance.setModal((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class WindowNodeHandler extends
		ContainerNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Window.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Window instance= java.awt.Window.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"size",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"visible");
					if(opt.isPresent()) instance.setVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"bounds",java.awt.Rectangle.class);
					if(opt.isPresent()) instance.setBounds(java.awt.Rectangle.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					/* skip setType : java.awt.Window$Type */
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"minimumSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setMinimumSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"background",java.awt.Color.class);
					if(opt.isPresent()) instance.setBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"location",java.awt.Point.class);
					if(opt.isPresent()) instance.setLocation(java.awt.Point.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusCycleRoot");
					if(opt.isPresent()) instance.setFocusCycleRoot((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"cursor",java.awt.Cursor.class);
					if(opt.isPresent()) instance.setCursor(java.awt.Cursor.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"locationByPlatform");
					if(opt.isPresent()) instance.setLocationByPlatform((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"opacity");
					if(opt.isPresent()) instance.setOpacity((float)opt.getAsDouble());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"shape",java.awt.Shape.class);
					if(opt.isPresent()) instance.setShape(java.awt.Shape.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"alwaysOnTop");
					if(opt.isPresent()) instance.setAlwaysOnTop((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"iconImages",java.util.List.class);
					if(opt.isPresent()) instance.setIconImages(java.util.List.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					/* skip setModalExclusionType : java.awt.Dialog$ModalExclusionType */
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"iconImage",java.awt.Image.class);
					if(opt.isPresent()) instance.setIconImage(java.awt.Image.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusableWindowState");
					if(opt.isPresent()) instance.setFocusableWindowState((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"autoRequestFocus");
					if(opt.isPresent()) instance.setAutoRequestFocus((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"locationRelativeTo",java.awt.Component.class);
					if(opt.isPresent()) instance.setLocationRelativeTo(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class DimensionNodeHandler extends
		Dimension2DNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Dimension.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Dimension instance= java.awt.Dimension.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"size",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class ContainerNodeHandler extends
		ComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Container.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Container instance= java.awt.Container.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layout",java.awt.LayoutManager.class);
					if(opt.isPresent()) instance.setLayout(java.awt.LayoutManager.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"font",java.awt.Font.class);
					if(opt.isPresent()) instance.setFont(java.awt.Font.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"focusTraversalPolicy",java.awt.FocusTraversalPolicy.class);
					if(opt.isPresent()) instance.setFocusTraversalPolicy(java.awt.FocusTraversalPolicy.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusCycleRoot");
					if(opt.isPresent()) instance.setFocusCycleRoot((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusTraversalPolicyProvider");
					if(opt.isPresent()) instance.setFocusTraversalPolicyProvider((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class ComponentNodeHandler extends
		NodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Component.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Component instance= java.awt.Component.class.cast(instance_o);
			
			
				{
				
					final Optional<String> opt = findString(root,"name");
					if(opt.isPresent()) instance.setName(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"locale",java.util.Locale.class);
					if(opt.isPresent()) instance.setLocale(java.util.Locale.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"size",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"visible");
					if(opt.isPresent()) instance.setVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"bounds",java.awt.Rectangle.class);
					if(opt.isPresent()) instance.setBounds(java.awt.Rectangle.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"font",java.awt.Font.class);
					if(opt.isPresent()) instance.setFont(java.awt.Font.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"preferredSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setPreferredSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"maximumSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setMaximumSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"minimumSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setMinimumSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"enabled");
					if(opt.isPresent()) instance.setEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"foreground",java.awt.Color.class);
					if(opt.isPresent()) instance.setForeground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"background",java.awt.Color.class);
					if(opt.isPresent()) instance.setBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"location",java.awt.Point.class);
					if(opt.isPresent()) instance.setLocation(java.awt.Point.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"componentOrientation",java.awt.ComponentOrientation.class);
					if(opt.isPresent()) instance.setComponentOrientation(java.awt.ComponentOrientation.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"dropTarget",java.awt.dnd.DropTarget.class);
					if(opt.isPresent()) instance.setDropTarget(java.awt.dnd.DropTarget.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"cursor",java.awt.Cursor.class);
					if(opt.isPresent()) instance.setCursor(java.awt.Cursor.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"ignoreRepaint");
					if(opt.isPresent()) instance.setIgnoreRepaint((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusable");
					if(opt.isPresent()) instance.setFocusable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusTraversalKeysEnabled");
					if(opt.isPresent()) instance.setFocusTraversalKeysEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"mixingCutoutShape",java.awt.Shape.class);
					if(opt.isPresent()) instance.setMixingCutoutShape(java.awt.Shape.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class FrameNodeHandler extends
		WindowNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Frame.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Frame instance= java.awt.Frame.class.cast(instance_o);
			
			
				{
				
					final OptionalInt opt = findInt(root,"state");
					if(opt.isPresent()) instance.setState(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"background",java.awt.Color.class);
					if(opt.isPresent()) instance.setBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"cursor");
					if(opt.isPresent()) instance.setCursor(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"undecorated");
					if(opt.isPresent()) instance.setUndecorated((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"title");
					if(opt.isPresent()) instance.setTitle(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"resizable");
					if(opt.isPresent()) instance.setResizable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"opacity");
					if(opt.isPresent()) instance.setOpacity((float)opt.getAsDouble());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"shape",java.awt.Shape.class);
					if(opt.isPresent()) instance.setShape(java.awt.Shape.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"iconImage",java.awt.Image.class);
					if(opt.isPresent()) instance.setIconImage(java.awt.Image.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"menuBar",java.awt.MenuBar.class);
					if(opt.isPresent()) instance.setMenuBar(java.awt.MenuBar.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"maximizedBounds",java.awt.Rectangle.class);
					if(opt.isPresent()) instance.setMaximizedBounds(java.awt.Rectangle.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"extendedState");
					if(opt.isPresent()) instance.setExtendedState(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class PointNodeHandler extends
		Point2DNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.Point.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.Point instance= java.awt.Point.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"location",java.awt.Point.class);
					if(opt.isPresent()) instance.setLocation(java.awt.Point.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class BorderLayoutNodeHandler extends
		NodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.BorderLayout.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.BorderLayout instance= java.awt.BorderLayout.class.cast(instance_o);
			
			
				{
				
					final OptionalInt opt = findInt(root,"hgap");
					if(opt.isPresent()) instance.setHgap(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"vgap");
					if(opt.isPresent()) instance.setVgap(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class Point2DNodeHandler extends
		NodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.geom.Point2D.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.geom.Point2D instance= java.awt.geom.Point2D.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"location",java.awt.geom.Point2D.class);
					if(opt.isPresent()) instance.setLocation(java.awt.geom.Point2D.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class Dimension2DNodeHandler extends
		NodeHandler {
		

		@Override
		public Class<?> getType()  {
			return java.awt.geom.Dimension2D.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final java.awt.geom.Dimension2D instance= java.awt.geom.Dimension2D.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"size",java.awt.geom.Dimension2D.class);
					if(opt.isPresent()) instance.setSize(java.awt.geom.Dimension2D.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JTextComponentNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.text.JTextComponent.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.text.JTextComponent instance= javax.swing.text.JTextComponent.class.cast(instance_o);
			
			
				{
				
					final Optional<String> opt = findString(root,"text");
					if(opt.isPresent()) instance.setText(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.TextUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.TextUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"componentOrientation",java.awt.ComponentOrientation.class);
					if(opt.isPresent()) instance.setComponentOrientation(java.awt.ComponentOrientation.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"document",javax.swing.text.Document.class);
					if(opt.isPresent()) instance.setDocument(javax.swing.text.Document.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"editable");
					if(opt.isPresent()) instance.setEditable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"dragEnabled");
					if(opt.isPresent()) instance.setDragEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"caretPosition");
					if(opt.isPresent()) instance.setCaretPosition(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"caret",javax.swing.text.Caret.class);
					if(opt.isPresent()) instance.setCaret(javax.swing.text.Caret.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"margin",java.awt.Insets.class);
					if(opt.isPresent()) instance.setMargin(java.awt.Insets.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"navigationFilter",javax.swing.text.NavigationFilter.class);
					if(opt.isPresent()) instance.setNavigationFilter(javax.swing.text.NavigationFilter.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"highlighter",javax.swing.text.Highlighter.class);
					if(opt.isPresent()) instance.setHighlighter(javax.swing.text.Highlighter.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"keymap",javax.swing.text.Keymap.class);
					if(opt.isPresent()) instance.setKeymap(javax.swing.text.Keymap.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"dropMode",javax.swing.DropMode.class);
					if(opt.isPresent()) instance.setDropMode(javax.swing.DropMode.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"caretColor",java.awt.Color.class);
					if(opt.isPresent()) instance.setCaretColor(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionColor",java.awt.Color.class);
					if(opt.isPresent()) instance.setSelectionColor(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectedTextColor",java.awt.Color.class);
					if(opt.isPresent()) instance.setSelectedTextColor(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"disabledTextColor",java.awt.Color.class);
					if(opt.isPresent()) instance.setDisabledTextColor(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Character> opt = findCharacter(root,"focusAccelerator");
					if(opt.isPresent()) instance.setFocusAccelerator(opt.get().charValue());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectionStart");
					if(opt.isPresent()) instance.setSelectionStart(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectionEnd");
					if(opt.isPresent()) instance.setSelectionEnd(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JComponentNodeHandler extends
		ContainerNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JComponent.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JComponent instance= javax.swing.JComponent.class.cast(instance_o);
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"opaque");
					if(opt.isPresent()) instance.setOpaque((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"visible");
					if(opt.isPresent()) instance.setVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"font",java.awt.Font.class);
					if(opt.isPresent()) instance.setFont(java.awt.Font.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"nextFocusableComponent",java.awt.Component.class);
					if(opt.isPresent()) instance.setNextFocusableComponent(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"preferredSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setPreferredSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"maximumSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setMaximumSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"minimumSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setMinimumSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"actionMap",javax.swing.ActionMap.class);
					if(opt.isPresent()) instance.setActionMap(javax.swing.ActionMap.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"enabled");
					if(opt.isPresent()) instance.setEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"foreground",java.awt.Color.class);
					if(opt.isPresent()) instance.setForeground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"background",java.awt.Color.class);
					if(opt.isPresent()) instance.setBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"autoscrolls");
					if(opt.isPresent()) instance.setAutoscrolls((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"inheritsPopupMenu");
					if(opt.isPresent()) instance.setInheritsPopupMenu((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"componentPopupMenu",javax.swing.JPopupMenu.class);
					if(opt.isPresent()) instance.setComponentPopupMenu(javax.swing.JPopupMenu.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"requestFocusEnabled");
					if(opt.isPresent()) instance.setRequestFocusEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"verifyInputWhenFocusTarget");
					if(opt.isPresent()) instance.setVerifyInputWhenFocusTarget((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"border",javax.swing.border.Border.class);
					if(opt.isPresent()) instance.setBorder(javax.swing.border.Border.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"alignmentY");
					if(opt.isPresent()) instance.setAlignmentY((float)opt.getAsDouble());
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"alignmentX");
					if(opt.isPresent()) instance.setAlignmentX((float)opt.getAsDouble());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"inputVerifier",javax.swing.InputVerifier.class);
					if(opt.isPresent()) instance.setInputVerifier(javax.swing.InputVerifier.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"debugGraphicsOptions");
					if(opt.isPresent()) instance.setDebugGraphicsOptions(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"toolTipText");
					if(opt.isPresent()) instance.setToolTipText(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"transferHandler",javax.swing.TransferHandler.class);
					if(opt.isPresent()) instance.setTransferHandler(javax.swing.TransferHandler.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"doubleBuffered");
					if(opt.isPresent()) instance.setDoubleBuffered((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JDialogNodeHandler extends
		DialogNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JDialog.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JDialog instance= javax.swing.JDialog.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layout",java.awt.LayoutManager.class);
					if(opt.isPresent()) instance.setLayout(java.awt.LayoutManager.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"transferHandler",javax.swing.TransferHandler.class);
					if(opt.isPresent()) instance.setTransferHandler(javax.swing.TransferHandler.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"JMenuBar",javax.swing.JMenuBar.class);
					if(opt.isPresent()) instance.setJMenuBar(javax.swing.JMenuBar.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"contentPane",java.awt.Container.class);
					if(opt.isPresent()) instance.setContentPane(java.awt.Container.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layeredPane",javax.swing.JLayeredPane.class);
					if(opt.isPresent()) instance.setLayeredPane(javax.swing.JLayeredPane.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"glassPane",java.awt.Component.class);
					if(opt.isPresent()) instance.setGlassPane(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"defaultCloseOperation");
					if(opt.isPresent()) instance.setDefaultCloseOperation(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JSliderNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JSlider.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JSlider instance= javax.swing.JSlider.class.cast(instance_o);
			
			
				{
				
					final OptionalInt opt = findInt(root,"value");
					if(opt.isPresent()) instance.setValue(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.SliderUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.SliderUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"orientation");
					if(opt.isPresent()) instance.setOrientation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"font",java.awt.Font.class);
					if(opt.isPresent()) instance.setFont(java.awt.Font.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.BoundedRangeModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.BoundedRangeModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"extent");
					if(opt.isPresent()) instance.setExtent(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"minimum");
					if(opt.isPresent()) instance.setMinimum(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"maximum");
					if(opt.isPresent()) instance.setMaximum(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"valueIsAdjusting");
					if(opt.isPresent()) instance.setValueIsAdjusting((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"labelTable",java.util.Dictionary.class);
					if(opt.isPresent()) instance.setLabelTable(java.util.Dictionary.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"inverted");
					if(opt.isPresent()) instance.setInverted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"majorTickSpacing");
					if(opt.isPresent()) instance.setMajorTickSpacing(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"minorTickSpacing");
					if(opt.isPresent()) instance.setMinorTickSpacing(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"snapToTicks");
					if(opt.isPresent()) instance.setSnapToTicks((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"paintTicks");
					if(opt.isPresent()) instance.setPaintTicks((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"paintTrack");
					if(opt.isPresent()) instance.setPaintTrack((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"paintLabels");
					if(opt.isPresent()) instance.setPaintLabels((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JMenuItemNodeHandler extends
		AbstractButtonNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JMenuItem.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JMenuItem instance= javax.swing.JMenuItem.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.MenuItemUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.MenuItemUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"enabled");
					if(opt.isPresent()) instance.setEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.ButtonModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.ButtonModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"armed");
					if(opt.isPresent()) instance.setArmed((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"accelerator",javax.swing.KeyStroke.class);
					if(opt.isPresent()) instance.setAccelerator(javax.swing.KeyStroke.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JMenuNodeHandler extends
		JMenuItemNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JMenu.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JMenu instance= javax.swing.JMenu.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"componentOrientation",java.awt.ComponentOrientation.class);
					if(opt.isPresent()) instance.setComponentOrientation(java.awt.ComponentOrientation.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.ButtonModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.ButtonModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"selected");
					if(opt.isPresent()) instance.setSelected((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"accelerator",javax.swing.KeyStroke.class);
					if(opt.isPresent()) instance.setAccelerator(javax.swing.KeyStroke.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"popupMenuVisible");
					if(opt.isPresent()) instance.setPopupMenuVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"delay");
					if(opt.isPresent()) instance.setDelay(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JMenuBarNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JMenuBar.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JMenuBar instance= javax.swing.JMenuBar.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.MenuBarUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.MenuBarUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"margin",java.awt.Insets.class);
					if(opt.isPresent()) instance.setMargin(java.awt.Insets.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selected",java.awt.Component.class);
					if(opt.isPresent()) instance.setSelected(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"borderPainted");
					if(opt.isPresent()) instance.setBorderPainted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionModel",javax.swing.SingleSelectionModel.class);
					if(opt.isPresent()) instance.setSelectionModel(javax.swing.SingleSelectionModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"helpMenu",javax.swing.JMenu.class);
					if(opt.isPresent()) instance.setHelpMenu(javax.swing.JMenu.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JTextAreaNodeHandler extends
		JTextComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JTextArea.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JTextArea instance= javax.swing.JTextArea.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"font",java.awt.Font.class);
					if(opt.isPresent()) instance.setFont(java.awt.Font.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"columns");
					if(opt.isPresent()) instance.setColumns(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"tabSize");
					if(opt.isPresent()) instance.setTabSize(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"lineWrap");
					if(opt.isPresent()) instance.setLineWrap((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"wrapStyleWord");
					if(opt.isPresent()) instance.setWrapStyleWord((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"rows");
					if(opt.isPresent()) instance.setRows(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JLayeredPaneNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JLayeredPane.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JLayeredPane instance= javax.swing.JLayeredPane.class.cast(instance_o);
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JDesktopPaneNodeHandler extends
		JLayeredPaneNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JDesktopPane.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JDesktopPane instance= javax.swing.JDesktopPane.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.DesktopPaneUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.DesktopPaneUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"dragMode");
					if(opt.isPresent()) instance.setDragMode(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"desktopManager",javax.swing.DesktopManager.class);
					if(opt.isPresent()) instance.setDesktopManager(javax.swing.DesktopManager.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectedFrame",javax.swing.JInternalFrame.class);
					if(opt.isPresent()) instance.setSelectedFrame(javax.swing.JInternalFrame.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JTextFieldNodeHandler extends
		JTextComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JTextField.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JTextField instance= javax.swing.JTextField.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"font",java.awt.Font.class);
					if(opt.isPresent()) instance.setFont(java.awt.Font.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"document",javax.swing.text.Document.class);
					if(opt.isPresent()) instance.setDocument(javax.swing.text.Document.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"action",javax.swing.Action.class);
					if(opt.isPresent()) instance.setAction(javax.swing.Action.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"actionCommand");
					if(opt.isPresent()) instance.setActionCommand(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"horizontalAlignment");
					if(opt.isPresent()) instance.setHorizontalAlignment(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"columns");
					if(opt.isPresent()) instance.setColumns(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"scrollOffset");
					if(opt.isPresent()) instance.setScrollOffset(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JTableNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JTable.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JTable instance= javax.swing.JTable.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.TableUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.TableUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"dragEnabled");
					if(opt.isPresent()) instance.setDragEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"dropMode",javax.swing.DropMode.class);
					if(opt.isPresent()) instance.setDropMode(javax.swing.DropMode.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.table.TableModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.table.TableModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionModel",javax.swing.ListSelectionModel.class);
					if(opt.isPresent()) instance.setSelectionModel(javax.swing.ListSelectionModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectionMode");
					if(opt.isPresent()) instance.setSelectionMode(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionForeground",java.awt.Color.class);
					if(opt.isPresent()) instance.setSelectionForeground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionBackground",java.awt.Color.class);
					if(opt.isPresent()) instance.setSelectionBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"rowHeight");
					if(opt.isPresent()) instance.setRowHeight(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"cellEditor",javax.swing.table.TableCellEditor.class);
					if(opt.isPresent()) instance.setCellEditor(javax.swing.table.TableCellEditor.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"columnModel",javax.swing.table.TableColumnModel.class);
					if(opt.isPresent()) instance.setColumnModel(javax.swing.table.TableColumnModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"rowMargin");
					if(opt.isPresent()) instance.setRowMargin(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"showHorizontalLines");
					if(opt.isPresent()) instance.setShowHorizontalLines((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"showVerticalLines");
					if(opt.isPresent()) instance.setShowVerticalLines((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"rowSorter",javax.swing.RowSorter.class);
					if(opt.isPresent()) instance.setRowSorter(javax.swing.RowSorter.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"columnSelectionAllowed");
					if(opt.isPresent()) instance.setColumnSelectionAllowed((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"rowSelectionAllowed");
					if(opt.isPresent()) instance.setRowSelectionAllowed((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"autoResizeMode");
					if(opt.isPresent()) instance.setAutoResizeMode(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"editingRow");
					if(opt.isPresent()) instance.setEditingRow(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"editingColumn");
					if(opt.isPresent()) instance.setEditingColumn(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"tableHeader",javax.swing.table.JTableHeader.class);
					if(opt.isPresent()) instance.setTableHeader(javax.swing.table.JTableHeader.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"showGrid");
					if(opt.isPresent()) instance.setShowGrid((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"surrendersFocusOnKeystroke");
					if(opt.isPresent()) instance.setSurrendersFocusOnKeystroke((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"preferredScrollableViewportSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setPreferredScrollableViewportSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"intercellSpacing",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setIntercellSpacing(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"gridColor",java.awt.Color.class);
					if(opt.isPresent()) instance.setGridColor(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"autoCreateColumnsFromModel");
					if(opt.isPresent()) instance.setAutoCreateColumnsFromModel((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"autoCreateRowSorter");
					if(opt.isPresent()) instance.setAutoCreateRowSorter((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"updateSelectionOnSort");
					if(opt.isPresent()) instance.setUpdateSelectionOnSort((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"cellSelectionEnabled");
					if(opt.isPresent()) instance.setCellSelectionEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"fillsViewportHeight");
					if(opt.isPresent()) instance.setFillsViewportHeight((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JPanelNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JPanel.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JPanel instance= javax.swing.JPanel.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.PanelUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.PanelUI.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JCheckBoxMenuItemNodeHandler extends
		JMenuItemNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JCheckBoxMenuItem.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JCheckBoxMenuItem instance= javax.swing.JCheckBoxMenuItem.class.cast(instance_o);
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"state");
					if(opt.isPresent()) instance.setState((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JSeparatorNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JSeparator.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JSeparator instance= javax.swing.JSeparator.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.SeparatorUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.SeparatorUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"orientation");
					if(opt.isPresent()) instance.setOrientation(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JTabbedPaneNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JTabbedPane.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JTabbedPane instance= javax.swing.JTabbedPane.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.TabbedPaneUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.TabbedPaneUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.SingleSelectionModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.SingleSelectionModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"tabPlacement");
					if(opt.isPresent()) instance.setTabPlacement(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"tabLayoutPolicy");
					if(opt.isPresent()) instance.setTabLayoutPolicy(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectedIndex");
					if(opt.isPresent()) instance.setSelectedIndex(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectedComponent",java.awt.Component.class);
					if(opt.isPresent()) instance.setSelectedComponent(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JListNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JList.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JList instance= javax.swing.JList.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.ListUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.ListUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"dragEnabled");
					if(opt.isPresent()) instance.setDragEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"dropMode",javax.swing.DropMode.class);
					if(opt.isPresent()) instance.setDropMode(javax.swing.DropMode.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.ListModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.ListModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectedIndex");
					if(opt.isPresent()) instance.setSelectedIndex(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionModel",javax.swing.ListSelectionModel.class);
					if(opt.isPresent()) instance.setSelectionModel(javax.swing.ListSelectionModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectedIndices",int[].class);
					if(opt.isPresent()) instance.setSelectedIndices(int[].class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectionMode");
					if(opt.isPresent()) instance.setSelectionMode(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"valueIsAdjusting");
					if(opt.isPresent()) instance.setValueIsAdjusting((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"prototypeCellValue",java.lang.Object.class);
					if(opt.isPresent()) instance.setPrototypeCellValue(java.lang.Object.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"fixedCellWidth");
					if(opt.isPresent()) instance.setFixedCellWidth(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"fixedCellHeight");
					if(opt.isPresent()) instance.setFixedCellHeight(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"cellRenderer",javax.swing.ListCellRenderer.class);
					if(opt.isPresent()) instance.setCellRenderer(javax.swing.ListCellRenderer.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionForeground",java.awt.Color.class);
					if(opt.isPresent()) instance.setSelectionForeground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionBackground",java.awt.Color.class);
					if(opt.isPresent()) instance.setSelectionBackground(java.awt.Color.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"visibleRowCount");
					if(opt.isPresent()) instance.setVisibleRowCount(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"layoutOrientation");
					if(opt.isPresent()) instance.setLayoutOrientation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"listData",java.util.Vector.class);
					if(opt.isPresent()) instance.setListData(java.util.Vector.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"listData",java.lang.Object[].class);
					if(opt.isPresent()) instance.setListData(java.lang.Object[].class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JTreeNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JTree.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JTree instance= javax.swing.JTree.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.TreeUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.TreeUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"editable");
					if(opt.isPresent()) instance.setEditable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"dragEnabled");
					if(opt.isPresent()) instance.setDragEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"dropMode",javax.swing.DropMode.class);
					if(opt.isPresent()) instance.setDropMode(javax.swing.DropMode.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.tree.TreeModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.tree.TreeModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionModel",javax.swing.tree.TreeSelectionModel.class);
					if(opt.isPresent()) instance.setSelectionModel(javax.swing.tree.TreeSelectionModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"cellRenderer",javax.swing.tree.TreeCellRenderer.class);
					if(opt.isPresent()) instance.setCellRenderer(javax.swing.tree.TreeCellRenderer.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"visibleRowCount");
					if(opt.isPresent()) instance.setVisibleRowCount(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"rootVisible");
					if(opt.isPresent()) instance.setRootVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"showsRootHandles");
					if(opt.isPresent()) instance.setShowsRootHandles((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionPaths",javax.swing.tree.TreePath[].class);
					if(opt.isPresent()) instance.setSelectionPaths(javax.swing.tree.TreePath[].class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"anchorSelectionPath",javax.swing.tree.TreePath.class);
					if(opt.isPresent()) instance.setAnchorSelectionPath(javax.swing.tree.TreePath.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"leadSelectionPath",javax.swing.tree.TreePath.class);
					if(opt.isPresent()) instance.setLeadSelectionPath(javax.swing.tree.TreePath.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionPath",javax.swing.tree.TreePath.class);
					if(opt.isPresent()) instance.setSelectionPath(javax.swing.tree.TreePath.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionRows",int[].class);
					if(opt.isPresent()) instance.setSelectionRows(int[].class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"rowHeight");
					if(opt.isPresent()) instance.setRowHeight(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"scrollsOnExpand");
					if(opt.isPresent()) instance.setScrollsOnExpand((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"cellEditor",javax.swing.tree.TreeCellEditor.class);
					if(opt.isPresent()) instance.setCellEditor(javax.swing.tree.TreeCellEditor.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"largeModel");
					if(opt.isPresent()) instance.setLargeModel((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"invokesStopCellEditing");
					if(opt.isPresent()) instance.setInvokesStopCellEditing((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"toggleClickCount");
					if(opt.isPresent()) instance.setToggleClickCount(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"expandsSelectedPaths");
					if(opt.isPresent()) instance.setExpandsSelectedPaths((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectionRow");
					if(opt.isPresent()) instance.setSelectionRow(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class AbstractButtonNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.AbstractButton.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.AbstractButton instance= javax.swing.AbstractButton.class.cast(instance_o);
			
			
				{
				
					final Optional<String> opt = findString(root,"text");
					if(opt.isPresent()) instance.setText(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layout",java.awt.LayoutManager.class);
					if(opt.isPresent()) instance.setLayout(java.awt.LayoutManager.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.ButtonUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.ButtonUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"enabled");
					if(opt.isPresent()) instance.setEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"action",javax.swing.Action.class);
					if(opt.isPresent()) instance.setAction(javax.swing.Action.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"actionCommand");
					if(opt.isPresent()) instance.setActionCommand(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"horizontalAlignment");
					if(opt.isPresent()) instance.setHorizontalAlignment(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"margin",java.awt.Insets.class);
					if(opt.isPresent()) instance.setMargin(java.awt.Insets.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.ButtonModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.ButtonModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"selected");
					if(opt.isPresent()) instance.setSelected((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"rolloverEnabled");
					if(opt.isPresent()) instance.setRolloverEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"hideActionText");
					if(opt.isPresent()) instance.setHideActionText((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"displayedMnemonicIndex");
					if(opt.isPresent()) instance.setDisplayedMnemonicIndex(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"mnemonic");
					if(opt.isPresent()) instance.setMnemonic(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Character> opt = findCharacter(root,"mnemonic");
					if(opt.isPresent()) instance.setMnemonic(opt.get().charValue());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"icon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"disabledIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setDisabledIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"disabledSelectedIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setDisabledSelectedIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"borderPainted");
					if(opt.isPresent()) instance.setBorderPainted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"iconTextGap");
					if(opt.isPresent()) instance.setIconTextGap(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"contentAreaFilled");
					if(opt.isPresent()) instance.setContentAreaFilled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"pressedIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setPressedIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectedIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setSelectedIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"rolloverIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setRolloverIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"rolloverSelectedIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setRolloverSelectedIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"verticalAlignment");
					if(opt.isPresent()) instance.setVerticalAlignment(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"verticalTextPosition");
					if(opt.isPresent()) instance.setVerticalTextPosition(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"horizontalTextPosition");
					if(opt.isPresent()) instance.setHorizontalTextPosition(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"focusPainted");
					if(opt.isPresent()) instance.setFocusPainted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalLong opt = findLong(root,"multiClickThreshhold");
					if(opt.isPresent()) instance.setMultiClickThreshhold(opt.getAsLong());
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"label");
					if(opt.isPresent()) instance.setLabel(String.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JPopupMenuNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JPopupMenu.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JPopupMenu instance= javax.swing.JPopupMenu.class.cast(instance_o);
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"visible");
					if(opt.isPresent()) instance.setVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.PopupMenuUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.PopupMenuUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selected",java.awt.Component.class);
					if(opt.isPresent()) instance.setSelected(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"borderPainted");
					if(opt.isPresent()) instance.setBorderPainted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"label");
					if(opt.isPresent()) instance.setLabel(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"lightWeightPopupEnabled");
					if(opt.isPresent()) instance.setLightWeightPopupEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectionModel",javax.swing.SingleSelectionModel.class);
					if(opt.isPresent()) instance.setSelectionModel(javax.swing.SingleSelectionModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"invoker",java.awt.Component.class);
					if(opt.isPresent()) instance.setInvoker(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"popupSize",java.awt.Dimension.class);
					if(opt.isPresent()) instance.setPopupSize(java.awt.Dimension.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JToggleButtonNodeHandler extends
		AbstractButtonNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JToggleButton.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JToggleButton instance= javax.swing.JToggleButton.class.cast(instance_o);
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JFrameNodeHandler extends
		FrameNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JFrame.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JFrame instance= javax.swing.JFrame.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layout",java.awt.LayoutManager.class);
					if(opt.isPresent()) instance.setLayout(java.awt.LayoutManager.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"transferHandler",javax.swing.TransferHandler.class);
					if(opt.isPresent()) instance.setTransferHandler(javax.swing.TransferHandler.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"JMenuBar",javax.swing.JMenuBar.class);
					if(opt.isPresent()) instance.setJMenuBar(javax.swing.JMenuBar.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"contentPane",java.awt.Container.class);
					if(opt.isPresent()) instance.setContentPane(java.awt.Container.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layeredPane",javax.swing.JLayeredPane.class);
					if(opt.isPresent()) instance.setLayeredPane(javax.swing.JLayeredPane.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"glassPane",java.awt.Component.class);
					if(opt.isPresent()) instance.setGlassPane(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"defaultCloseOperation");
					if(opt.isPresent()) instance.setDefaultCloseOperation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"iconImage",java.awt.Image.class);
					if(opt.isPresent()) instance.setIconImage(java.awt.Image.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JPasswordFieldNodeHandler extends
		JTextFieldNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JPasswordField.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JPasswordField instance= javax.swing.JPasswordField.class.cast(instance_o);
			
			
				{
				
					final Optional<String> opt = findString(root,"text");
					if(opt.isPresent()) instance.setText(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Character> opt = findCharacter(root,"echoChar");
					if(opt.isPresent()) instance.setEchoChar(opt.get().charValue());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JRadioButtonNodeHandler extends
		JToggleButtonNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JRadioButton.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JRadioButton instance= javax.swing.JRadioButton.class.cast(instance_o);
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JLabelNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JLabel.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JLabel instance= javax.swing.JLabel.class.cast(instance_o);
			
			
				{
				
					final Optional<String> opt = findString(root,"text");
					if(opt.isPresent()) instance.setText(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.LabelUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.LabelUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"horizontalAlignment");
					if(opt.isPresent()) instance.setHorizontalAlignment(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"displayedMnemonicIndex");
					if(opt.isPresent()) instance.setDisplayedMnemonicIndex(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"icon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"disabledIcon",javax.swing.Icon.class);
					if(opt.isPresent()) instance.setDisabledIcon(javax.swing.Icon.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"iconTextGap");
					if(opt.isPresent()) instance.setIconTextGap(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"verticalAlignment");
					if(opt.isPresent()) instance.setVerticalAlignment(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"verticalTextPosition");
					if(opt.isPresent()) instance.setVerticalTextPosition(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"horizontalTextPosition");
					if(opt.isPresent()) instance.setHorizontalTextPosition(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Character> opt = findCharacter(root,"displayedMnemonic");
					if(opt.isPresent()) instance.setDisplayedMnemonic(opt.get().charValue());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"displayedMnemonic");
					if(opt.isPresent()) instance.setDisplayedMnemonic(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"labelFor",java.awt.Component.class);
					if(opt.isPresent()) instance.setLabelFor(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JCheckBoxNodeHandler extends
		JToggleButtonNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JCheckBox.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JCheckBox instance= javax.swing.JCheckBox.class.cast(instance_o);
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"borderPaintedFlat");
					if(opt.isPresent()) instance.setBorderPaintedFlat((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JScrollPaneNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JScrollPane.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JScrollPane instance= javax.swing.JScrollPane.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"layout",java.awt.LayoutManager.class);
					if(opt.isPresent()) instance.setLayout(java.awt.LayoutManager.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.ScrollPaneUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.ScrollPaneUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"componentOrientation",java.awt.ComponentOrientation.class);
					if(opt.isPresent()) instance.setComponentOrientation(java.awt.ComponentOrientation.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"verticalScrollBarPolicy");
					if(opt.isPresent()) instance.setVerticalScrollBarPolicy(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"horizontalScrollBarPolicy");
					if(opt.isPresent()) instance.setHorizontalScrollBarPolicy(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"viewport",javax.swing.JViewport.class);
					if(opt.isPresent()) instance.setViewport(javax.swing.JViewport.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"verticalScrollBar",javax.swing.JScrollBar.class);
					if(opt.isPresent()) instance.setVerticalScrollBar(javax.swing.JScrollBar.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"horizontalScrollBar",javax.swing.JScrollBar.class);
					if(opt.isPresent()) instance.setHorizontalScrollBar(javax.swing.JScrollBar.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"viewportView",java.awt.Component.class);
					if(opt.isPresent()) instance.setViewportView(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"rowHeader",javax.swing.JViewport.class);
					if(opt.isPresent()) instance.setRowHeader(javax.swing.JViewport.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"columnHeader",javax.swing.JViewport.class);
					if(opt.isPresent()) instance.setColumnHeader(javax.swing.JViewport.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"viewportBorder",javax.swing.border.Border.class);
					if(opt.isPresent()) instance.setViewportBorder(javax.swing.border.Border.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"rowHeaderView",java.awt.Component.class);
					if(opt.isPresent()) instance.setRowHeaderView(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"columnHeaderView",java.awt.Component.class);
					if(opt.isPresent()) instance.setColumnHeaderView(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"wheelScrollingEnabled");
					if(opt.isPresent()) instance.setWheelScrollingEnabled((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JSplitPaneNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JSplitPane.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JSplitPane instance= javax.swing.JSplitPane.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"leftComponent",java.awt.Component.class);
					if(opt.isPresent()) instance.setLeftComponent(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"rightComponent",java.awt.Component.class);
					if(opt.isPresent()) instance.setRightComponent(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.SplitPaneUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.SplitPaneUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"dividerLocation");
					if(opt.isPresent()) instance.setDividerLocation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"dividerLocation");
					if(opt.isPresent()) instance.setDividerLocation(opt.getAsDouble());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"lastDividerLocation");
					if(opt.isPresent()) instance.setLastDividerLocation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"dividerSize");
					if(opt.isPresent()) instance.setDividerSize(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"oneTouchExpandable");
					if(opt.isPresent()) instance.setOneTouchExpandable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"topComponent",java.awt.Component.class);
					if(opt.isPresent()) instance.setTopComponent(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"bottomComponent",java.awt.Component.class);
					if(opt.isPresent()) instance.setBottomComponent(java.awt.Component.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"orientation");
					if(opt.isPresent()) instance.setOrientation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"continuousLayout");
					if(opt.isPresent()) instance.setContinuousLayout((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalDouble opt = findDouble(root,"resizeWeight");
					if(opt.isPresent()) instance.setResizeWeight(opt.getAsDouble());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JProgressBarNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JProgressBar.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JProgressBar instance= javax.swing.JProgressBar.class.cast(instance_o);
			
			
				{
				
					final OptionalInt opt = findInt(root,"value");
					if(opt.isPresent()) instance.setValue(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.ProgressBarUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.ProgressBarUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"orientation");
					if(opt.isPresent()) instance.setOrientation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.BoundedRangeModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.BoundedRangeModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"borderPainted");
					if(opt.isPresent()) instance.setBorderPainted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"stringPainted");
					if(opt.isPresent()) instance.setStringPainted((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"string");
					if(opt.isPresent()) instance.setString(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"indeterminate");
					if(opt.isPresent()) instance.setIndeterminate((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"minimum");
					if(opt.isPresent()) instance.setMinimum(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"maximum");
					if(opt.isPresent()) instance.setMaximum(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JButtonNodeHandler extends
		AbstractButtonNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JButton.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JButton instance= javax.swing.JButton.class.cast(instance_o);
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"defaultCapable");
					if(opt.isPresent()) instance.setDefaultCapable((boolean)opt.get());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JComboBoxNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JComboBox.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JComboBox instance= javax.swing.JComboBox.class.cast(instance_o);
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.ComboBoxUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.ComboBoxUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"enabled");
					if(opt.isPresent()) instance.setEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"action",javax.swing.Action.class);
					if(opt.isPresent()) instance.setAction(javax.swing.Action.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<String> opt = findString(root,"actionCommand");
					if(opt.isPresent()) instance.setActionCommand(String.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"editable");
					if(opt.isPresent()) instance.setEditable((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.ComboBoxModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.ComboBoxModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"selectedIndex");
					if(opt.isPresent()) instance.setSelectedIndex(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"selectedItem",java.lang.Object.class);
					if(opt.isPresent()) instance.setSelectedItem(java.lang.Object.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"popupVisible");
					if(opt.isPresent()) instance.setPopupVisible((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"lightWeightPopupEnabled");
					if(opt.isPresent()) instance.setLightWeightPopupEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"maximumRowCount");
					if(opt.isPresent()) instance.setMaximumRowCount(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"renderer",javax.swing.ListCellRenderer.class);
					if(opt.isPresent()) instance.setRenderer(javax.swing.ListCellRenderer.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"editor",javax.swing.ComboBoxEditor.class);
					if(opt.isPresent()) instance.setEditor(javax.swing.ComboBoxEditor.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"prototypeDisplayValue",java.lang.Object.class);
					if(opt.isPresent()) instance.setPrototypeDisplayValue(java.lang.Object.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					/* skip setKeySelectionManager : javax.swing.JComboBox$KeySelectionManager */
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	
	protected class JScrollBarNodeHandler extends
		JComponentNodeHandler {
		

		@Override
		public Class<?> getType()  {
			return javax.swing.JScrollBar.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final javax.swing.JScrollBar instance= javax.swing.JScrollBar.class.cast(instance_o);
			
			
				{
				
					final OptionalInt opt = findInt(root,"value");
					if(opt.isPresent()) instance.setValue(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"UI",javax.swing.plaf.ScrollBarUI.class);
					if(opt.isPresent()) instance.setUI(javax.swing.plaf.ScrollBarUI.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"orientation");
					if(opt.isPresent()) instance.setOrientation(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"enabled");
					if(opt.isPresent()) instance.setEnabled((boolean)opt.get());
					
				}
			
			
			
				{
				
					final Optional<Object> opt = findObject(root,"model",javax.swing.BoundedRangeModel.class);
					if(opt.isPresent()) instance.setModel(javax.swing.BoundedRangeModel.class.cast(opt.get()));
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"minimum");
					if(opt.isPresent()) instance.setMinimum(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"maximum");
					if(opt.isPresent()) instance.setMaximum(opt.getAsInt());
					
				}
			
			
			
				{
				
					final Optional<Boolean> opt = findBoolean(root,"valueIsAdjusting");
					if(opt.isPresent()) instance.setValueIsAdjusting((boolean)opt.get());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"unitIncrement");
					if(opt.isPresent()) instance.setUnitIncrement(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"blockIncrement");
					if(opt.isPresent()) instance.setBlockIncrement(opt.getAsInt());
					
				}
			
			
			
				{
				
					final OptionalInt opt = findInt(root,"visibleAmount");
					if(opt.isPresent()) instance.setVisibleAmount(opt.getAsInt());
					
				}
			
			
			super.fillSetters(instance,root);
			}
		}
	

	protected BaseSwingXmlContext() {
	}

	@Override
	public Logger getLogger() {
		return LOG;
		}

	@Override
	protected void registerNodeHandlers() {
		super.registerNodeHandlers();
		
		registerNodeHandler(new DialogNodeHandler());
		
		registerNodeHandler(new WindowNodeHandler());
		
		registerNodeHandler(new DimensionNodeHandler());
		
		registerNodeHandler(new ContainerNodeHandler());
		
		registerNodeHandler(new FrameNodeHandler());
		
		registerNodeHandler(new PointNodeHandler());
		
		registerNodeHandler(new BorderLayoutNodeHandler());
		
		registerNodeHandler(new JDialogNodeHandler());
		
		registerNodeHandler(new JSliderNodeHandler());
		
		registerNodeHandler(new JMenuItemNodeHandler());
		
		registerNodeHandler(new JMenuNodeHandler());
		
		registerNodeHandler(new JMenuBarNodeHandler());
		
		registerNodeHandler(new JTextAreaNodeHandler());
		
		registerNodeHandler(new JLayeredPaneNodeHandler());
		
		registerNodeHandler(new JDesktopPaneNodeHandler());
		
		registerNodeHandler(new JTextFieldNodeHandler());
		
		registerNodeHandler(new JTableNodeHandler());
		
		registerNodeHandler(new JPanelNodeHandler());
		
		registerNodeHandler(new JCheckBoxMenuItemNodeHandler());
		
		registerNodeHandler(new JSeparatorNodeHandler());
		
		registerNodeHandler(new JTabbedPaneNodeHandler());
		
		registerNodeHandler(new JListNodeHandler());
		
		registerNodeHandler(new JTreeNodeHandler());
		
		registerNodeHandler(new JPopupMenuNodeHandler());
		
		registerNodeHandler(new JToggleButtonNodeHandler());
		
		registerNodeHandler(new JFrameNodeHandler());
		
		registerNodeHandler(new JPasswordFieldNodeHandler());
		
		registerNodeHandler(new JRadioButtonNodeHandler());
		
		registerNodeHandler(new JLabelNodeHandler());
		
		registerNodeHandler(new JCheckBoxNodeHandler());
		
		registerNodeHandler(new JScrollPaneNodeHandler());
		
		registerNodeHandler(new JSplitPaneNodeHandler());
		
		registerNodeHandler(new JProgressBarNodeHandler());
		
		registerNodeHandler(new JButtonNodeHandler());
		
		registerNodeHandler(new JComboBoxNodeHandler());
		
		registerNodeHandler(new JScrollBarNodeHandler());
		
	}

	


}
