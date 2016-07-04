/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.jackrabbit.oak.plugins.multiplex;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.apache.jackrabbit.oak.spi.mount.Mount;

import static org.apache.jackrabbit.oak.commons.PathUtils.isAncestor;

final class MountInfo {
    
    private static final Function<String, String> SANITIZE_PATH =  new Function<String,String>() {
        @Override
        public String apply(String input) {
            if ( input.endsWith("/") && input.length() > 1) {
                return input.substring(0, input.length() - 2); 
            }
            return input;
        }
    };
    
    private final Mount mount;
    private final List<String> includedPaths;

    public MountInfo(Mount mount, List<String> includedPaths){
        this.mount = mount;
        this.includedPaths = cleanCopy(includedPaths);
    }

    public Mount getMount() {
        return mount;
    }

    public boolean isMounted(String path){
        if (path.contains(mount.getPathFragmentName())){
            return true;
        }

        path = SANITIZE_PATH.apply(path);

        //TODO may be optimized via trie
        
        for (String includedPath : includedPaths){
            if (includedPath.equals(path) || isAncestor(includedPath, path)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isWrapped(String from, String to) {
        for ( String path : includedPaths ) {
            if ( from.compareTo(path) < 0 && path.compareTo(to) < 0 ) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(mount);
        for (String path : includedPaths) {
            pw.printf("\t%s%n", path);
        }
        return sw.toString();
    }
    
    private ImmutableList<String> cleanCopy(List<String> includedPaths) {
        // ensure that paths don't have trailing slashes - this triggers an assertion in PahtUtils isAncestor
        return ImmutableList.copyOf(Iterables.transform(includedPaths, SANITIZE_PATH));
    }
}