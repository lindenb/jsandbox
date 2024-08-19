<?xml version='1.0' encoding="UTF-8"?>
<xsl:stylesheet
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
	version='1.0'
	>

<!--

convert output of xml2java to constructors for XMLSwingFactory

-->
<xsl:output method="text" version="1.0" encoding="ASCII"/>

<xsl:template match="/">
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

	<xsl:for-each select="packages/package/classes/class">
	protected class <xsl:value-of select="@simpleName"/>NodeHandler extends
		<xsl:choose>
			<xsl:when test="not(@extends) or @extends='java.lang.Object'">NodeHandler</xsl:when>
			<xsl:otherwise><xsl:call-template name="substring-after-last">
            <xsl:with-param name="input" select="@extends" />
            <xsl:with-param name="marker" select="'.'" />
        </xsl:call-template>NodeHandler</xsl:otherwise>
		</xsl:choose> {
		

		@Override
		public Class&lt;?&gt; getType()  {
			return <xsl:value-of select="@name"/>.class;
			}
		

		@Override
		protected void fillSetters(final Object instance_o,final Element root) {
			final <xsl:value-of select="@name"/> instance= <xsl:value-of select="@name"/>.class.cast(instance_o);
			<xsl:for-each select="methods/method[@declared='true' and @setter='true']">
			
				{
				<xsl:choose>
					<xsl:when test="parameters/parameter/@type='byte'">
					final OptionalInt opt = findByte(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>((byte)opt.getAsInt());
					</xsl:when>				
				
					<xsl:when test="parameters/parameter/@type='char'">
					final Optional&lt;Character&gt; opt = findCharacter(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>(opt.get().charValue());
					</xsl:when>	
			
					<xsl:when test="parameters/parameter/@type='short'">
					final OptionalInt opt = findShort(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>((short)opt.getAsInt());
					</xsl:when>			
			
					<xsl:when test="parameters/parameter/@type='int'">
					final OptionalInt opt = findInt(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>(opt.getAsInt());
					</xsl:when>

					<xsl:when test="parameters/parameter/@type='long'">
					final OptionalLong opt = findLong(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>(opt.getAsLong());
					</xsl:when>
					
					
					<xsl:when test="parameters/parameter/@type='double' or parameters/parameter/@type='float'">
					final OptionalDouble opt = findDouble(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>(<xsl:if test="parameters/parameter/@type='float'">(float)</xsl:if>opt.getAsDouble());
					</xsl:when>
					
					<xsl:when test="parameters/parameter/@type='boolean'">
					final Optional&lt;Boolean&gt; opt = findBoolean(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>((boolean)opt.get());
					</xsl:when>
					
					
					<xsl:when test="parameters/parameter/@type='java.lang.String'">
					final Optional&lt;String&gt; opt = findString(root,"<xsl:value-of select="@setter-name"/>");
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>(String.class.cast(opt.get()));
					</xsl:when>
					
					<xsl:when test="contains(parameters/parameter/@type,'$')">
					/* skip <xsl:value-of select="@name"/> : <xsl:value-of select="parameters/parameter/@type"/> */
					</xsl:when>
					
					<xsl:otherwise>
					final Optional&lt;Object&gt; opt = findObject(root,"<xsl:value-of select="@setter-name"/>",<xsl:value-of select="parameters/parameter/@type"/>.class);
					if(opt.isPresent()) instance.<xsl:value-of select="@name"/>(<xsl:value-of select="parameters/parameter/@type"/>.class.cast(opt.get()));
					</xsl:otherwise>
					
				</xsl:choose>
				}
			
			</xsl:for-each>
			super.fillSetters(instance,root);
			}
		}
	</xsl:for-each>

	protected BaseSwingXmlContext() {
	}

	@Override
	public Logger getLogger() {
		return LOG;
		}

	@Override
	protected void registerNodeHandlers() {
		super.registerNodeHandlers();
		<xsl:for-each select="packages/package/classes/class[@abstract='false']">
		registerNodeHandler(new <xsl:value-of select="@simpleName"/>NodeHandler());
		</xsl:for-each>
	}

	


}
</xsl:template>


<xsl:template name="substring-after-last">
        <xsl:param name="input" />
        <xsl:param name="marker" />
        <xsl:choose>
            <xsl:when test="contains($input,$marker)">
                <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="input"
          select="substring-after($input,$marker)" />
                    <xsl:with-param name="marker" select="$marker" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$input" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>


