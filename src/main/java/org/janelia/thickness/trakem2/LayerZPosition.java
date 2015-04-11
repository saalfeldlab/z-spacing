/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.thickness.trakem2;

import ij.gui.GenericDialog;
import ini.trakem2.Project;
import ini.trakem2.display.Display;
import ini.trakem2.display.Displayable;
import ini.trakem2.display.Layer;
import ini.trakem2.display.LayerSet;
import ini.trakem2.plugin.TPlugIn;
import ini.trakem2.utils.Utils;

import java.awt.Rectangle;
import java.util.List;

/**
 *
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class LayerZPosition implements TPlugIn
{
	protected LayerSet layerset = null;

	private Layer currentLayer( final Object... params )
	{
		final Layer layer;
		if (params != null && params[ 0 ] != null)
		{
			final Object param = params[ 0 ];
			if ( Layer.class.isInstance( param.getClass() ) )
				layer = ( Layer)param;
			else if ( LayerSet.class.isInstance( param.getClass() ) )
				layer = ( ( LayerSet )param ).getLayer( 0 );
			else if ( Displayable.class.isInstance( param.getClass() ) )
				layer = ( ( Displayable )param ).getLayerSet().getLayer( 0 );
			else layer = null;
		}
		else
		{
			final Display front = Display.getFront();
			if ( front == null )
				layer = Project.getProjects().get(0).getRootLayerSet().getLayer( 0 );
			else
				layer = front.getLayer();
		}
		return layer;
	}

	@Override
	public boolean setup( final Object... params )
	{
		if (params != null && params[ 0 ] != null)
		{
			final Object param = params[ 0 ];
			if ( LayerSet.class.isInstance( param.getClass() ) )
				layerset = ( LayerSet )param;
			else if ( Displayable.class.isInstance( param.getClass() ) )
				layerset = ( ( Displayable )param ).getLayerSet();
			else
				return false;
		}
		else
		{
			final Display front = Display.getFront();
			if ( front == null )
				layerset = Project.getProjects().get(0).getRootLayerSet();
			else
				layerset = front.getLayerSet();
		}
		return true;
	}

	public void invokeSIFT( final List< Layer > layers, final Rectangle fov )
	{

	}

	public void invokeNCC( final List< Layer > layers, final Rectangle fov )
	{

	}

	@Override
	public Object invoke( final Object... params )
	{
		if ( !setup( params ) )
			return null;

		final Layer layer = currentLayer( params );
		final GenericDialog gd = new GenericDialog( "Correct layer z-positions" );
		Utils.addLayerRangeChoices( layer, gd );
		gd.addChoice(
				"Simiarity_method : ",
				new String[]{ "NCC (aligned)", "SIFT consensus (unaligned)" }, "NCC (aligned)" );
		gd.showDialog();
		if ( gd.wasCanceled() )
			return null;

		final List< Layer > layers =
				layerset.getLayers().subList(
						gd.getNextChoiceIndex(),
						gd.getNextChoiceIndex() + 1 );
		final int method = gd.getNextChoiceIndex();
		switch ( method )
		{
		case 1:
			invokeSIFT( layers );
		default :
			invokeNCC( layers );
			break;
		}

		return null;
	}

	@Override
	public boolean applies( final Object ob )
	{
		return true;
	}
}
