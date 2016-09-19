
# react-native-binary-put

## Getting started

`$ npm install react-native-binary-put --save`

### Mostly automatic installation

`$ react-native link react-native-binary-put`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-binary-put` and add `RNBinaryPut.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNBinaryPut.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNBinaryPutPackage;` to the imports at the top of the file
  - Add `new RNBinaryPutPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-binary-put'
  	project(':react-native-binary-put').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-binary-put/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-binary-put')
  	```
## Usage
```javascript
import RNBinaryPut from 'react-native-binary-put';

const sourceUrl = "http://...";
const targetUrl = "http://my.service/image/123"
const authHeader = null;
const contentType = 'image/png';

RNBinaryPut.put(sourceUrl, targetUrl, contentType, authHeader, (error, success) => {
  if(error) {
    console.warn("upload failed", error);    
  } else {
    console.warn("upload success", success);        
  }
});
```
  