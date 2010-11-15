/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.extensions;

import org.apache.fop.fo.pagination.SimplePageMaster;

/**
 * This interface may be implemented by extension attachments that apply to PageViewport areas,
 * in which case the extension attachment is given an opportunity to determine if it should
 * be included in the page viewport's extensions.
 */
public interface FilterPageExtensionAttachment {

    /**
     * This method determines if the extension should NOT be
     * included in the set of extension attachments added to a page viewport area.
     * @param spm the simple page master that applies to the page viewport area
     * and from which the extension attachment is obtained
     * @param pageNumber the page number
     * @return true if this extension attachment should NOT be included (i.e.,
     * should be filtered out) from the extensions to be added to the page viewport
     * area
     */
    boolean filter ( SimplePageMaster spm, int pageNumber );

}
