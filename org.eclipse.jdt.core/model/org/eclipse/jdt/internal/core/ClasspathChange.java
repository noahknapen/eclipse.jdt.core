/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.core.util.Util;

public class ClasspathChange {
	JavaProject project;
	IClasspathEntry[] oldRawClasspath;
	IPath oldOutputLocation;
	IClasspathEntry[] oldResolvedClasspath;
	
	public ClasspathChange(JavaProject project, IClasspathEntry[] oldRawClasspath, IPath oldOutputLocation, IClasspathEntry[] oldResolvedClasspath) {
		this.project = project;
		this.oldRawClasspath = oldRawClasspath;
		this.oldOutputLocation = oldOutputLocation;
		this.oldResolvedClasspath = oldResolvedClasspath;
	}
	
	private void addClasspathDeltas(JavaElementDelta delta, IPackageFragmentRoot[] roots, int flag) {
		for (int i = 0; i < roots.length; i++) {
			IPackageFragmentRoot root = roots[i];
			delta.changed(root, flag);
			if ((flag & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0 
					|| (flag & IJavaElementDelta.F_SOURCEATTACHED) != 0
					|| (flag & IJavaElementDelta.F_SOURCEDETACHED) != 0){
				try {
					root.close();
				} catch (JavaModelException e) {
					// ignore
				}
				// force detach source on jar package fragment roots (source will be lazily computed when needed)
				((PackageFragmentRoot) root).setSourceAttachmentProperty(null);// loose info - will be recomputed
			}
		}
	}

	/*
	 * Returns the index of the item in the list if the given list contains the specified entry. If the list does
	 * not contain the entry, -1 is returned.
	 */
	private int classpathContains(IClasspathEntry[] list, IClasspathEntry entry) {
		IPath[] exclusionPatterns = entry.getExclusionPatterns();
		IPath[] inclusionPatterns = entry.getInclusionPatterns();
		nextEntry: for (int i = 0; i < list.length; i++) {
			IClasspathEntry other = list[i];
			if (other.getContentKind() == entry.getContentKind()
				&& other.getEntryKind() == entry.getEntryKind()
				&& other.isExported() == entry.isExported()
				&& other.getPath().equals(entry.getPath())) {
					// check custom outputs
					IPath entryOutput = entry.getOutputLocation();
					IPath otherOutput = other.getOutputLocation();
					if (entryOutput == null) {
						if (otherOutput != null)
							continue;
					} else {
						if (!entryOutput.equals(otherOutput))
							continue;
					}
					
					// check inclusion patterns
					IPath[] otherIncludes = other.getInclusionPatterns();
					if (inclusionPatterns != otherIncludes) {
					    if (inclusionPatterns == null) continue;
						int includeLength = inclusionPatterns.length;
						if (otherIncludes == null || otherIncludes.length != includeLength)
							continue;
						for (int j = 0; j < includeLength; j++) {
							// compare toStrings instead of IPaths 
							// since IPath.equals is specified to ignore trailing separators
							if (!inclusionPatterns[j].toString().equals(otherIncludes[j].toString()))
								continue nextEntry;
						}
					}
					// check exclusion patterns
					IPath[] otherExcludes = other.getExclusionPatterns();
					if (exclusionPatterns != otherExcludes) {
					    if (exclusionPatterns == null) continue;
						int excludeLength = exclusionPatterns.length;
						if (otherExcludes == null || otherExcludes.length != excludeLength)
							continue;
						for (int j = 0; j < excludeLength; j++) {
							// compare toStrings instead of IPaths 
							// since IPath.equals is specified to ignore trailing separators
							if (!exclusionPatterns[j].toString().equals(otherExcludes[j].toString()))
								continue nextEntry;
						}
					}
					return i;
			}
		}
		return -1;
	}

	/*
	 * Recursively adds all subfolders of <code>folder</code> to the given collection.
	 */
	private void collectAllSubfolders(IFolder folder, ArrayList collection) throws JavaModelException {
		try {
			IResource[] members= folder.members();
			for (int i = 0, max = members.length; i < max; i++) {
				IResource r= members[i];
				if (r.getType() == IResource.FOLDER) {
					collection.add(r);
					collectAllSubfolders((IFolder)r, collection);
				}
			}	
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}

	/*
	 * Returns a collection of package fragments that have been added/removed
	 * as the result of changing the output location to/from the given
	 * location. The collection is empty if no package fragments are
	 * affected.
	 */
	private ArrayList determineAffectedPackageFragments(IPath location) throws JavaModelException {
		ArrayList fragments = new ArrayList();
	
		// see if this will cause any package fragments to be affected
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = null;
		if (location != null) {
			resource = workspace.getRoot().findMember(location);
		}
		if (resource != null && resource.getType() == IResource.FOLDER) {
			IFolder folder = (IFolder) resource;
			// only changes if it actually existed
			IClasspathEntry[] classpath = this.project.getExpandedClasspath();
			for (int i = 0; i < classpath.length; i++) {
				IClasspathEntry entry = classpath[i];
				IPath path = classpath[i].getPath();
				if (entry.getEntryKind() != IClasspathEntry.CPE_PROJECT && path.isPrefixOf(location) && !path.equals(location)) {
					IPackageFragmentRoot[] roots = this.project.computePackageFragmentRoots(classpath[i]);
					PackageFragmentRoot root = (PackageFragmentRoot) roots[0];
					// now the output location becomes a package fragment - along with any subfolders
					ArrayList folders = new ArrayList();
					folders.add(folder);
					collectAllSubfolders(folder, folders);
					Iterator elements = folders.iterator();
					int segments = path.segmentCount();
					while (elements.hasNext()) {
						IFolder f = (IFolder) elements.next();
						IPath relativePath = f.getFullPath().removeFirstSegments(segments);
						String[] pkgName = relativePath.segments();
						IPackageFragment pkg = root.getPackageFragment(pkgName);
						if (!Util.isExcluded(pkg))
							fragments.add(pkg);
					}
				}
			}
		}
		return fragments;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof ClasspathChange))
			return false;
		return this.project.equals(((ClasspathChange) obj).project);
	}

	/*
	 * Generates a classpath change delta for this classpath change.
	 * Returns whether a delta was generated.
	 */
	public boolean generateDelta(JavaElementDelta delta) {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		DeltaProcessingState state = manager.deltaState;
		if (state.findJavaProject(this.project.getElementName()) == null)
			// project doesn't exist yet (we're in an IWorkspaceRunnable)
			// no need to create a delta here and no need to index (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133334)
			// the delta processor will create an ADDED project delta, and index the project
			return false;

		DeltaProcessor deltaProcessor = state.getDeltaProcessor();
		IClasspathEntry[] newResolvedClasspath = null;
		IPath newOutputLocation = null;
		boolean hasDelta = false;
		try {
			PerProjectInfo perProjectInfo = this.project.getPerProjectInfo();
			
			// get new info
			this.project.resolveClasspath(perProjectInfo);
			IClasspathEntry[] newRawClasspath;
			
			// use synchronized block to ensure consistency
			synchronized (perProjectInfo) {
				newRawClasspath = perProjectInfo.rawClasspath;
				newResolvedClasspath = perProjectInfo.resolvedClasspath;
				newOutputLocation = perProjectInfo.outputLocation;				
			}
			
			// check if raw classpath has changed
			if (this.oldRawClasspath != null && !JavaProject.areClasspathsEqual(this.oldRawClasspath, newRawClasspath, this.oldOutputLocation, newOutputLocation)) {
				delta.changed(this.project, IJavaElementDelta.F_CLASSPATH_CHANGED);
				hasDelta = true;
			}
					
			// if no changes to resolved classpath, nothing more to do
			if (this.oldResolvedClasspath != null && JavaProject.areClasspathsEqual(this.oldResolvedClasspath, newResolvedClasspath, this.oldOutputLocation, newOutputLocation))
				return false;
			
			// close cached info
			this.project.close();
		} catch (JavaModelException e) {	
			if (DeltaProcessor.VERBOSE) {
				e.printStackTrace();
			}
		}
		
		if (this.oldResolvedClasspath == null)
			return false;
		
		Map removedRoots = null;
		IPackageFragmentRoot[] roots = null;
		Map allOldRoots ;
		if ((allOldRoots = deltaProcessor.oldRoots) != null) {
	 		roots = (IPackageFragmentRoot[]) allOldRoots.get(this.project);
		}
		if (roots != null) {
			removedRoots = new HashMap();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				removedRoots.put(root.getPath(), root);
			}
		}

		int newLength = newResolvedClasspath.length;
		int oldLength = this.oldResolvedClasspath.length;		
		for (int i = 0; i < oldLength; i++) {
			int index = classpathContains(newResolvedClasspath, this.oldResolvedClasspath[i]);
			if (index == -1) {
				// do not notify remote project changes
				if (this.oldResolvedClasspath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT){
					continue; 
				}

				IPackageFragmentRoot[] pkgFragmentRoots = null;
				if (removedRoots != null) {
					IPackageFragmentRoot oldRoot = (IPackageFragmentRoot)  removedRoots.get(this.oldResolvedClasspath[i].getPath());
					if (oldRoot != null) { // use old root if any (could be none if entry wasn't bound)
						pkgFragmentRoots = new IPackageFragmentRoot[] { oldRoot };
					}
				}
				if (pkgFragmentRoots == null) {
					try {
						ObjectVector accumulatedRoots = new ObjectVector();
						HashSet rootIDs = new HashSet(5);
						rootIDs.add(this.project.rootID());
						this.project.computePackageFragmentRoots(
							this.oldResolvedClasspath[i], 
							accumulatedRoots, 
							rootIDs,
							null, // inside original project
							false, // don't check existency
							false, // don't retrieve exported roots
							null); /*no reverse map*/
						pkgFragmentRoots = new IPackageFragmentRoot[accumulatedRoots.size()];
						accumulatedRoots.copyInto(pkgFragmentRoots);
					} catch (JavaModelException e) {
						pkgFragmentRoots =  new IPackageFragmentRoot[] {};
					}
				}
				addClasspathDeltas(delta, pkgFragmentRoots, IJavaElementDelta.F_REMOVED_FROM_CLASSPATH);
				hasDelta = true;
			} else {
				// do not notify remote project changes
				if (this.oldResolvedClasspath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					continue; 
				}				
				if (index != i) { //reordering of the classpath
					addClasspathDeltas(delta, this.project.computePackageFragmentRoots(this.oldResolvedClasspath[i]),	IJavaElementDelta.F_REORDER);
					hasDelta = true;
				}
				
				// check source attachment
				IPath newSourcePath = newResolvedClasspath[index].getSourceAttachmentPath();
				int sourceAttachmentFlags = getSourceAttachmentDeltaFlag(this.oldResolvedClasspath[i].getSourceAttachmentPath(), newSourcePath);
				IPath oldRootPath = this.oldResolvedClasspath[i].getSourceAttachmentRootPath();
				IPath newRootPath = newResolvedClasspath[index].getSourceAttachmentRootPath();
				int sourceAttachmentRootFlags = getSourceAttachmentDeltaFlag(oldRootPath, newRootPath);
				int flags = sourceAttachmentFlags | sourceAttachmentRootFlags;
				if (flags != 0) {
					addClasspathDeltas(delta, this.project.computePackageFragmentRoots(this.oldResolvedClasspath[i]), flags);
					hasDelta = true;
				} else {
					if (oldRootPath == null && newRootPath == null) {
						// if source path is specified and no root path, it needs to be recomputed dynamically
						// force detach source on jar package fragment roots (source will be lazily computed when needed)
						IPackageFragmentRoot[] computedRoots = this.project.computePackageFragmentRoots(this.oldResolvedClasspath[i]);
						for (int j = 0; j < computedRoots.length; j++) {
							IPackageFragmentRoot root = computedRoots[j];
							// force detach source on jar package fragment roots (source will be lazily computed when needed)
							try {
								root.close();
							} catch (JavaModelException e) {
								// ignore
							}
							((PackageFragmentRoot) root).setSourceAttachmentProperty(null);// loose info - will be recomputed
						}
					}
				}
			}
		}

		for (int i = 0; i < newLength; i++) {
			int index = classpathContains(this.oldResolvedClasspath, newResolvedClasspath[i]);
			if (index == -1) {
				// do not notify remote project changes
				if (newResolvedClasspath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT){
					continue; 
				}
				addClasspathDeltas(delta, this.project.computePackageFragmentRoots(newResolvedClasspath[i]), IJavaElementDelta.F_ADDED_TO_CLASSPATH);
				hasDelta = true;
			} // classpath reordering has already been generated in previous loop
		}

		// see if a change in output location will cause any package fragments to be added/removed
		if ((newOutputLocation == null && this.oldOutputLocation != null) 
				|| (newOutputLocation != null && !newOutputLocation.equals(this.oldOutputLocation))) {
			try {
				ArrayList added= determineAffectedPackageFragments(this.oldOutputLocation);
				Iterator iter = added.iterator();
				while (iter.hasNext()){
					IPackageFragment frag= (IPackageFragment)iter.next();
					((IPackageFragmentRoot)frag.getParent()).close();
					delta.added(frag);
					hasDelta = true;
				}
			
				// see if this will cause any package fragments to be removed
				ArrayList removed= determineAffectedPackageFragments(newOutputLocation);
				iter = removed.iterator();
				while (iter.hasNext()) {
					IPackageFragment frag= (IPackageFragment)iter.next();
					((IPackageFragmentRoot)frag.getParent()).close(); 
					delta.removed(frag);
					hasDelta = true;
				}
			} catch (JavaModelException e) {
				if (DeltaProcessor.VERBOSE)
					e.printStackTrace();
			}
		}

		return hasDelta;
	}
	
	/*
	 * Returns the source attachment flag for the delta between the 2 give source paths.
	 * Returns either F_SOURCEATTACHED, F_SOURCEDETACHED, F_SOURCEATTACHED | F_SOURCEDETACHED
	 * or 0 if there is no difference.
	 */
	private int getSourceAttachmentDeltaFlag(IPath oldPath, IPath newPath) {
		if (oldPath == null) {
			if (newPath != null) {
				return IJavaElementDelta.F_SOURCEATTACHED;
			} else {
				return 0;
			}
		} else if (newPath == null) {
			return IJavaElementDelta.F_SOURCEDETACHED;
		} else if (!oldPath.equals(newPath)) {
			return IJavaElementDelta.F_SOURCEATTACHED | IJavaElementDelta.F_SOURCEDETACHED;
		} else {
			return 0;
		}
	}
	
	public int hashCode() {
		return this.project.hashCode();
	}

	/*
	 * Request the indexing of entries that have been added, and remove the index for removed entries.
	 */
	public void requestIndexing() {
		IClasspathEntry[] newResolvedClasspath = null;
		try {
			newResolvedClasspath = this.project.getResolvedClasspath();
		} catch (JavaModelException e) {	
			// project doesn't exist
			return;
		}
		
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IndexManager indexManager = manager.indexManager;
		if (indexManager == null)
			return;
		DeltaProcessingState state = manager.deltaState;

		int newLength = newResolvedClasspath.length;
		int oldLength = this.oldResolvedClasspath.length;		
		for (int i = 0; i < oldLength; i++) {
			int index = classpathContains(newResolvedClasspath, this.oldResolvedClasspath[i]);
			if (index == -1) {
				// remote projects are not indexed in this project
				if (this.oldResolvedClasspath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT){
					continue; 
				}

				// Remove the .java files from the index for a source folder
				// For a lib folder or a .jar file, remove the corresponding index if not shared.
				IClasspathEntry oldEntry = this.oldResolvedClasspath[i];
				final IPath path = oldEntry.getPath();
				int changeKind = this.oldResolvedClasspath[i].getEntryKind();
				switch (changeKind) {
					case IClasspathEntry.CPE_SOURCE:
						char[][] inclusionPatterns = ((ClasspathEntry)oldEntry).fullInclusionPatternChars();
						char[][] exclusionPatterns = ((ClasspathEntry)oldEntry).fullExclusionPatternChars();
						indexManager.removeSourceFolderFromIndex(this.project, path, inclusionPatterns, exclusionPatterns);
						break;
					case IClasspathEntry.CPE_LIBRARY:
						if (state.otherRoots.get(path) == null) { // if root was not shared
							indexManager.discardJobs(path.toString());
							indexManager.removeIndex(path);
							// TODO (kent) we could just remove the in-memory index and have the indexing check for timestamps
						}
						break;
				}
			}
		}

		for (int i = 0; i < newLength; i++) {
			int index = classpathContains(this.oldResolvedClasspath, newResolvedClasspath[i]);
			if (index == -1) {
				// remote projects are not indexed in this project
				if (newResolvedClasspath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT){
					continue; 
				}
				
				// Request indexing
				int entryKind = newResolvedClasspath[i].getEntryKind();
				switch (entryKind) {
					case IClasspathEntry.CPE_LIBRARY:
						boolean pathHasChanged = true;
						IPath newPath = newResolvedClasspath[i].getPath();
						for (int j = 0; j < oldLength; j++) {
							IClasspathEntry oldEntry = this.oldResolvedClasspath[j];
							if (oldEntry.getPath().equals(newPath)) {
								pathHasChanged = false;
								break;
							}
						}
						if (pathHasChanged) {
							indexManager.indexLibrary(newPath, this.project.getProject());
						}
						break;
					case IClasspathEntry.CPE_SOURCE:
						IClasspathEntry entry = newResolvedClasspath[i];
						IPath path = entry.getPath();
						char[][] inclusionPatterns = ((ClasspathEntry)entry).fullInclusionPatternChars();
						char[][] exclusionPatterns = ((ClasspathEntry)entry).fullExclusionPatternChars();
						indexManager.indexSourceFolder(this.project, path, inclusionPatterns, exclusionPatterns);
						break;
				}
			}
		}
	}
}
