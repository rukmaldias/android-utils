/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.example.overlayservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class ProcessManager {
	
	Context mContext;
	List<String> mKnownPackages;
	List<String> mLaunchers;
	
	public NewProcessManager(Context context) {
		mContext = context;
		initialize();
	}
	
	private void initialize() {
		// Add known system applications with no UI
		mKnownPackages = new ArrayList<String>();
		mKnownPackages.add("com.android.systemui");
		mKnownPackages.add("org.cyanogenmod.audiofx");
		mKnownPackages.add("com.android.incallui");
		mKnownPackages.add("com.google.android.gms.persistent");
		mKnownPackages.add("org.cyanogenmod.theme.chooser");
		mKnownPackages.add("com.android.smspush");
		mKnownPackages.add("com.google.android.googlequicksearchbox:interactor");
		mKnownPackages.add("com.google.android.gms");
		
		// Add known launchers
		mLaunchers = new ArrayList<String>();
		mLaunchers.add("com.s7.galaxy.launcher");
		mLaunchers.add("com.android.launcher");
	}
	
	/**
	 * Returns visible application except the calling application.
	 * @return
	 */
	public synchronized String getVisibleApplication() {
		//StringBuffer output = new StringBuffer();
		String packageName = mContext.getPackageName();
		String result = null;
		try {
			Process process = Runtime.getRuntime().exec("/system/bin/toolbox ps -p -P -x -c");
			//Process process = Runtime.getRuntime().exec("/system/bin/toolbox ps -p -P -x -c |grep -E 'u0.*fg'");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			List<String> alllines = new ArrayList<String>();
			List<PkgInfo> packageInfoList = new ArrayList<NewProcessManager.PkgInfo>();
			String line = "";
			
			while ((line = reader.readLine()) != null) {
				alllines.add(line);
			}
			
            // Waits for the command to finish.
            process.waitFor();
            
            //1. grep -E 'u0.*fg',
            //2. remove processes WCHAN values != '00000000'
            
            for (String string : alllines) {
				if(string.startsWith("u0") && string.contains(" fg ")) {
					try {
						String[] arr = string.split("\\s+", 13);
						String s = arr[12];
						if(s != null) {
							String[] arr2 = s.split("\\s+");
							String pkg = arr2[2];
							if(pkg.contains(".")) {
								PkgInfo pi = new PkgInfo();
								pi.setName(pkg);
								packageInfoList.add(pi);
								
								String info = arr2[3];
								if(info != null) {
									String i_arr[] = info.split(":", 2);
									if(i_arr[1] != null) {
										String[] u_arr = i_arr[1].split(",");
										if(u_arr[0] != null) {
											try {
												int processU = Integer.valueOf(u_arr[0]);
												pi.setU(processU);
											}
											catch(NumberFormatException nfe) {
											}
										}
									}
								}
							}
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
            
            // if launcher presents -> no visible app
            // else find highest `u` in process info
            // ignore known packages with higher `u` values
            
            boolean foundLauncher = false;
            
            for (PkgInfo pkgInfo : packageInfoList) {
				if(mLaunchers.contains(pkgInfo.name)) {
					foundLauncher = true;
					break;
				}
			}
            
            int pos = -1;
            int value = 0;
            if(!foundLauncher) {
            	for (int i=0; i<packageInfoList.size(); i++) {
            		PkgInfo pkgInfo = packageInfoList.get(i);
            		if(!packageName.equals(pkgInfo.getName()) 
            				&& !mKnownPackages.contains(pkgInfo.getName()) 
            				&& pkgInfo.getU() > value) {
            			
            			value = pkgInfo.getU();
            			pos = i;
            		}
            	}
            }
            
            if(pos >= 0) {
            	PkgInfo pkg = packageInfoList.get(pos);
            	result = pkg.getName();
            }
		} 
		catch(InterruptedException ie) {
			ie.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private class PkgInfo {
		private String name;
		private int u;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getU() {
			return u;
		}
		public void setU(int u) {
			this.u = u;
		}
	}
	
}
