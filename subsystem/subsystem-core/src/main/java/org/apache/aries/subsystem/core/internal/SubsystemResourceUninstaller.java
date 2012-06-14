package org.apache.aries.subsystem.core.internal;

import org.apache.aries.util.io.IOUtils;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemResourceUninstaller extends ResourceUninstaller {
	private static final Logger logger = LoggerFactory.getLogger(AriesSubsystem.class);
	
	private static void removeChild(AriesSubsystem parent, AriesSubsystem child) {
		Activator.getInstance().getSubsystems().removeChild(parent, child);
	}
	
	public SubsystemResourceUninstaller(Resource resource, AriesSubsystem subsystem) {
		super(resource, subsystem);
	}
	
	public void uninstall() {
		if (isResourceUninstallable())
			uninstallSubsystem();
		removeReferences();
		removeConstituents();
		removeChildren();
	}
	
	private void removeChildren() {
		if (isImplicit()) {
			removeChild((AriesSubsystem)subsystem, (AriesSubsystem)resource);
			return;
		}
		for (Subsystem subsystem : ((AriesSubsystem)resource).getParents())
			removeChild((AriesSubsystem)subsystem, (AriesSubsystem)resource);
	}
	
	private void removeConstituents() {
		if (isImplicit()) {
			removeConstituent();
			return;
		}
		for (Subsystem subsystem : ((AriesSubsystem)resource).getParents())
			removeConstituent((AriesSubsystem)subsystem, (AriesSubsystem)resource);
	}
	
	private void removeReferences() {
		if (isImplicit()) {
			removeReference();
			return;
		}
		for (Subsystem subsystem : ((AriesSubsystem)resource).getParents())
			removeReference((AriesSubsystem)subsystem, (AriesSubsystem)resource);
	}
	
	private void uninstallSubsystem() {
		AriesSubsystem subsystem = (AriesSubsystem)resource;
		if (subsystem.getState().equals(Subsystem.State.RESOLVED))
				subsystem.setState(State.INSTALLED);
			subsystem.setState(State.UNINSTALLING);
			Throwable firstError = null;
			for (Resource resource : Activator.getInstance().getSubsystems().getResourcesReferencedBy(subsystem)) {
				// Don't uninstall the region context bundle here.
				if (ResourceHelper.getSymbolicNameAttribute(resource).startsWith(RegionContextBundleHelper.SYMBOLICNAME_PREFIX))
					continue;
				try {
					ResourceUninstaller.newInstance(resource, subsystem).uninstall();
				}
				catch (Throwable t) {
					logger.error("An error occurred while uninstalling resource " + resource + " of subsystem " + subsystem, t);
					if (firstError == null)
						firstError = t;
				}
			}
			IOUtils.deleteRecursive(subsystem.getDirectory());
			subsystem.setState(State.UNINSTALLED);
			Activator.getInstance().getSubsystemServiceRegistrar().unregister(subsystem);
			if (subsystem.isScoped())
				RegionContextBundleHelper.uninstallRegionContextBundle(subsystem);
			if (firstError != null)
				throw new SubsystemException(firstError);
	}
}