/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.state

/**
 * Class representing the state of an AEP container.
 *
 * This class includes the common property `loaded` which indicates whether the container's content has been loaded.
 * It can also be include additional properties in the future for functionality like filtering, sorting, etc.
 *
 * @property loaded Indicates whether the container's content has been loaded.
 */
open class AepContainerUIState(
    open val loaded: Boolean
)
