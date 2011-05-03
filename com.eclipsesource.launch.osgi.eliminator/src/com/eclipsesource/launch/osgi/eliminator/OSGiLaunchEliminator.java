/******************************************************************************* 
* Copyright (c) 2011 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*******************************************************************************/ 
package com.eclipsesource.launch.osgi.eliminator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.RuntimeProcess;


final class OSGiLaunchEliminator implements ILaunchListener {

  @Override
  public void launchAdded( ILaunch launch ) {
    try {
      if( isOSGiLaunch( launch ) ) {
        terminateIfRunning( launch );
      }
    } catch( CoreException e ) {
      handleTerminationError();
    }
  }
  
  private static boolean isOSGiLaunch( ILaunch launch ) throws CoreException {
    ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
    boolean result = false;
    if( launchConfiguration != null ) {
      ILaunchConfigurationType type = launchConfiguration.getType();
      String identifier = type.getIdentifier();
      if( identifier.equals( "org.eclipse.pde.ui.EquinoxLauncher" ) ) {
        result = true;
      }
    }
    return result;
  }

  private static void terminateIfRunning( ILaunch launch ) throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();
    String taskName = "Eliminate running instance";
    monitor.beginTask( taskName, IProgressMonitor.UNKNOWN );
    try {
      final ILaunch runningLaunch = findRunning( launch );
      if( runningLaunch != null ) {
        terminate( runningLaunch );
      }
    } finally {
      monitor.done();
    }
  }

  private static void terminate( final ILaunch previousLaunch ) throws DebugException {
    final Object signal = new Object();
    final boolean[] terminated = {
      false
    };
    DebugPlugin debugPlugin = DebugPlugin.getDefault();
    debugPlugin.addDebugEventListener( new IDebugEventSetListener() {

      public void handleDebugEvents( final DebugEvent[] events ) {
        for( int i = 0; i < events.length; i++ ) {
          DebugEvent event = events[ i ];
          if( isTerminateEventFor( event, previousLaunch ) ) {
            DebugPlugin.getDefault().removeDebugEventListener( this );
            synchronized( signal ) {
              terminated[ 0 ] = true;
              signal.notifyAll();
            }
          }
        }
      }
    } );
    previousLaunch.terminate();
    try {
      synchronized( signal ) {
        if( !terminated[ 0 ] ) {
          signal.wait();
        }
      }
    } catch( InterruptedException e ) {
      // ignore
    }
  }
  
  private static boolean isTerminateEventFor( DebugEvent event, ILaunch launch ) {
    boolean result = false;
    if(    event.getKind() == DebugEvent.TERMINATE 
        && event.getSource() instanceof RuntimeProcess ) 
    {
      RuntimeProcess process = ( RuntimeProcess )event.getSource();
      result = process.getLaunch() == launch;
    }
    return result;
  }

  private static ILaunch findRunning( ILaunch launch ) {
    ILaunchConfiguration config = launch.getLaunchConfiguration();
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunch[] runningLaunches = launchManager.getLaunches();
    ILaunch result = null;
    for( ILaunch runningLaunch : runningLaunches ) {
      if(    runningLaunch != launch 
          && config.getName().equals( getLaunchName( runningLaunch ) ) 
          && !runningLaunch.isTerminated() )
      {
        result = runningLaunch;  
      }
    }
    return result;
  }

  private static String getLaunchName( ILaunch launch ) {
    ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
    // the launch config might be null (e.g. if deleted) even though there
    // still exists a launch for that config  
    return launchConfiguration == null ? null : launchConfiguration.getName();
  }
  
  private static void handleTerminationError() {
    Activator plugin = Activator.getDefault();
    ILog log = plugin.getLog();
    String pluginId = plugin.getBundle().getSymbolicName();
    log.log( new Status( IStatus.ERROR, pluginId, "Could not eliminate OSGi launch" ) );
  }

  @Override
  public void launchRemoved( ILaunch launch ) {
    // do nothing
  }
  
  @Override
  public void launchChanged( ILaunch launch ) {
    // do nothing
  }
  
}
