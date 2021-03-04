## OpenDataCam Mobile v1.0

_OpenDataCam Mobile v1.0 was funded by [#MyGalileoSolution 2020 challenge](https://www.gsa.europa.eu/newsroom/news/mygalileosolution-projects-selected-acceleration), a context organized by [GSA, The European GNSS Agency](https://www.gsa.europa.eu/)_ 

Get it on Android https://play.google.com/store/apps/details?id=com.opendatacam

![optimized1](https://user-images.githubusercontent.com/533590/109954625-d819fe00-7ce1-11eb-85ae-ba7da9d6fef6.jpg)

![unnamed](https://user-images.githubusercontent.com/533590/109954630-d9e3c180-7ce1-11eb-9d6e-12c42488e1ac.jpg)

### OpenDataCam

OpenDataCam is an open source tool to quantify the world. It can detect, track and count objects on any video feed using AI. It is designed to be an accessible and affordable solution running locally on smartphones, desktop computers and IoT devices.

OpenDataCam never records any photo or video data. The system only saves surveyed meta-data, in particular the path an object moved or number of counted objects at a certain point. The novelty of OpenDataCam is, that everything happens on location, while no visual data is saved or sent to online cloud processing.

All software is based on open source components and runs completely locally. The software features a friendly user interface and is currently optimised for detecting and counting traffic participants, but is not limited to that.

Both software and hardware setup are documented and offered as an open source project, to underline transparency and full disclosure on privacy questions.

The OpenDataCam project respects data privacy in a transparent way. No data is required to be send to the cloud. OpenDataCam was successfully used for a wide range of use-cases from urban mobility, assets management to nature conservation.

### Technical notes

OpenDataCam Mobile v1.0 is running `YOLOv4-tiny` using NCNN on CPU (320 x 192 input size)

See [Development notes](DEVELOPER_NOTES.md) if you want to customize it for your use.

### External Dependencies

- Neural network inference framework NCNN : https://github.com/Tencent/ncnn (LICENSE BSD 3-Clause)
- Nodejs mobile : https://github.com/JaneaSystems/nodejs-mobile (LICENSE MIT)