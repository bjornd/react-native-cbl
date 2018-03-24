using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace React.Native.Cbl.RNReactNativeCbl
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNReactNativeCblModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNReactNativeCblModule"/>.
        /// </summary>
        internal RNReactNativeCblModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNReactNativeCbl";
            }
        }
    }
}
