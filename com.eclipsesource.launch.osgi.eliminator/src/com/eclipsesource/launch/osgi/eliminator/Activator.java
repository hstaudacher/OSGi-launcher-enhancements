/*******************************************************************************
 * Copyright (c) 2011 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * EclipseSource - initial API and implementation
 *******************************************************************************/
package com.eclipsesource.launch.osgi.eliminator;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public final class Activator extends AbstractUIPlugin implements IStartup {

  private static Activator plugin;
  private OSGiLaunchEliminator launchEliminator;

  static Activator getDefault() {
    return plugin;
  }

  @Override
  public void start( BundleContext bundleContext ) throws Exception {
    plugin = this;
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    launchEliminator = new OSGiLaunchEliminator();
    manager.addLaunchListener( launchEliminator );
  }

  @Override
  public void stop( BundleContext bundleContext ) throws Exception {
    plugin = null;
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    manager.removeLaunchListener( launchEliminator );
  }

  @Override
  public void earlyStartup() {
    // do nothing
  }
}
