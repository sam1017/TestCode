#include "image_refocus.h"
#include <sys/time.h>

#define LOG_TAG "Gallery2_Refocus_image_refocus"

#define MM_PROFILING

namespace android {

ImageRefocus::ImageRefocus() {
}

ImageRefocus::ImageRefocus(int jpsWidth, int jpsHeight, int maskWidth, int maskHeight, int posX, int posY,
        int viewWidth, int viewHeight, int orientation, int mainCamPos, int touchCoordX1st, int touchCoordY1st,
        int refocusMode) {
    debugConfig();
    mRefocusTuningInfo = {8, 16, 4, 0, 4, 1, 3.4};
    mRefocusImageInfo.ImgNum = 1;
    mRefocusImageInfo.ImgFmt = (MTK_REFOCUS_IMAGE_FMT_ENUM) UTL_IMAGE_FORMAT_YUV420;
    switch (refocusMode) {
    case 0:
        mRefocusImageInfo.Mode = REFOCUS_MODE_FULL;
        break;
    case 1:
        mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_ONLY;
        break;
    case 2:
        mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_AND_XMP;
        break;
    case 3:
        mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_AND_REFOCUS;
        break;
    case 4:
        mRefocusImageInfo.Mode = REFOCUS_MODE_MAX;
        break;
    default:
        break;
    }

    //test number for generate depth map buffer, maybe need optimization in future
    mRefocusImageInfo.TouchCoordX = touchCoordX1st;
    mRefocusImageInfo.TouchCoordY = touchCoordY1st;
    mRefocusImageInfo.DepthOfField = testDepthOfField;

    mRefocusImageInfo.Width = jpsWidth;
    mRefocusImageInfo.Height = jpsHeight;
    mRefocusImageInfo.MaskWidth = maskWidth;
    mRefocusImageInfo.MaskHeight = maskHeight;
    mRefocusImageInfo.PosX = posX;
    mRefocusImageInfo.PosY = posY;
    mRefocusImageInfo.ViewWidth = viewWidth;
    mRefocusImageInfo.ViewHeight = viewHeight;
    mRefocusImageInfo.JPSOrientation = REFOCUS_ORIENTATION_0;
    mRefocusImageInfo.JPGOrientation = REFOCUS_ORIENTATION_0;
    mRefocusImageInfo.MainCamPos = REFOCUS_MAINCAM_POS_ON_RIGHT;

    //default rectify info in dual cam refocus
    mRefocusImageInfo.RcfyError = DFT_RCFY_ERROR;
    mRefocusImageInfo.RcfyIterNo = DFT_RCFY_ITER_NO;
    mRefocusImageInfo.DisparityRange = DFT_DISPARITY_RANGE;
    mRefocusImageInfo.Theta[0] = DFT_THETA;
    mRefocusImageInfo.Theta[1] = DFT_THETA;
    mRefocusImageInfo.Theta[2] = DFT_THETA;
    mRefocusImageInfo.DepthBufferAddr = NULL;
    ALOGI("mRefocusImageInfo.RcfyError %d, ", mRefocusImageInfo.RcfyError);

    mRefocusTuningInfo.IterationTimes = 3;
    mRefocusTuningInfo.HorzDownSampleRatio = 8;
    mRefocusTuningInfo.VertDownSampleRatio = 8;
    mRefocusImageInfo.DRZ_WD = 960;
    mRefocusImageInfo.DRZ_HT = 540;

    mRefocusTuningInfo.Baseline = 2.0f;
    mRefocusTuningInfo.CoreNumber = 4;
}

bool ImageRefocus::initRefocusNoDepthMapTest(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int imgOrientation, const char *depthmapSourcePath, int inStereoImgWidth, int inStereoImgHeight,
        const char *maskFilePath) {
    ALOGI("image refocus initRefocusNoDepthMap start inStereoImgWidth %d, inStereoImgHeight %d, outImgWidth %d,"
            "outImgHeight %d", inStereoImgWidth, inStereoImgHeight, outImgWidth, outImgHeight);
    /* Image Information */
    mStereoImgWidth = inStereoImgWidth;
    mSetreoImgHeight = inStereoImgHeight;
    mOutTargetImgWidth = outImgWidth;
    mOutTargetImgHeight = outImgHeight;

    ALOGI("image refocus initRefocusIMGSource ");
    initRefocusIMGSource(sourceFilePath, outImgWidth, outImgHeight, imgOrientation);
    initRefocusDepthInfo(depthmapSourcePath, inStereoImgWidth, inStereoImgHeight);

    char filename[255];
    //1. jpg parse start
#ifdef MM_BMP_DEBUG
    BITMAP jpegBMP;
    sprintf(filename, "%s", sourceFilePath);
    if(bmp_parse(filename,&jpegBMP)!=0)
    {
        ALOGI("Reading jpeg image:%s......\n", filename);
        bmp_read(filename,&jpegBMP);
    }
    else
    {
        ALOGI("Fail to read jpeg image:%s!!!!!!\n", filename);
    }
    ALOGI("Jpeg image width = %d height = %d\n", jpegBMP.width, jpegBMP.height);
    MUINT8* jpegBuffer = new MUINT8 [jpegBMP.width * jpegBMP.height * 3 / 2];
    mRefocusImageInfo.TargetWidth = jpegBMP.width;
    mRefocusImageInfo.TargetHeight = jpegBMP.height;
    mRefocusImageInfo.TargetImgAddr = jpegBuffer;
    bmp_toYUV420(&jpegBMP,(unsigned char *)mRefocusImageInfo.TargetImgAddr);
    bmp_free(&jpegBMP);
    // jpg parse end
#endif

    //2. jps parse start
    BITMAP jpsBMP;
    sprintf(filename, "%s", depthmapSourcePath);
    if (bmp_parse(filename, &jpsBMP) != 0) {
        ALOGI("Reading jps image:%s......\n", filename);
        bmp_read(filename, &jpsBMP);
    } else {
        ALOGI("Fail to read jps image:%s!!!!!!\n", filename);
    }
    ALOGI("Jps image width = %d height = %d\n", jpsBMP.width, jpsBMP.height);
    MUINT8* jpsBuffer = new MUINT8[jpsBMP.width * jpsBMP.height * 3 / 2];
    mRefocusImageInfo.Width = jpsBMP.width;
    mRefocusImageInfo.Height = jpsBMP.height;
    mRefocusImageInfo.ImgAddr[0] = jpsBuffer;
    bmp_toYUV420(&jpsBMP, (unsigned char *) mRefocusImageInfo.ImgAddr[0]);
    bmp_free(&jpsBMP);

    //3. config parse start
    //parse_configuration("/sdcard/Pictures/Config.cfg");

    //4. mask parse start
    BITMAP maskBMP;
    sprintf(filename, "%s", maskFilePath);
    if (bmp_parse(filename, &maskBMP) != 0) {
        printf("Reading mask image:%s......\n", filename);
        bmp_read(filename, &maskBMP);
    }
    ALOGI("Mask image width = %d height = %d\n", maskBMP.width, maskBMP.height);
    MUINT8* maskBuffer = new MUINT8[maskBMP.width * maskBMP.height];
    mRefocusImageInfo.MaskWidth = maskBMP.width;
    mRefocusImageInfo.MaskHeight = maskBMP.height;
    mRefocusImageInfo.MaskImageAddr = maskBuffer;
    memcpy(maskBuffer, maskBMP.g, maskBMP.width * maskBMP.height);
    bmp_free(&maskBMP);
    // mask parse end

    if (createRefocusInstance() && setBufferAddr() && generate()) {
        ALOGI("image refocus init end, success");
        return SUCCESS;
    }
    ALOGI("image refocus init end, fail");
    return FAIL;
}

bool ImageRefocus::initRefocusNoDepthMapRealFileTest(const char *testSourceFilePath, const char *sourceFilePath,
        const char *jpsFilePath, int outImgWidth, int outImgHeight, int imgOrientation, MUINT8* jpsBuffer,
        int jpsBufferSize, int inStereoImgWidth, int inStereoImgHeight, MUINT8* maskBuffer, int maskBufferSize,
        int maskWidth, int maskHeight) {
    ALOGI("image refocus initRefocusNoDepthMap start inStereoImgWidth %d, inStereoImgHeight %d, outImgWidth %d,"
            "outImgHeight %d", inStereoImgWidth, inStereoImgHeight, outImgWidth, outImgHeight);
    /* Image Information */
    mStereoImgWidth = inStereoImgWidth;
    mSetreoImgHeight = inStereoImgHeight;

    ALOGI("image refocus initRefocusIMGSource ");
    initRefocusIMGSource(testSourceFilePath, outImgWidth, outImgHeight, imgOrientation);
    //2. jps parse start
    mRefocusImageInfo.Width = mStereoImgWidth;
    mRefocusImageInfo.Height = mSetreoImgHeight;
    mRefocusImageInfo.ImgAddr[0] = jpsBuffer;

    //initJPSIMGSource(jpsFilePath, mRefocusImageInfo.Width, mRefocusImageInfo.Height);
    initJPSBuffer(jpsBuffer, jpsBufferSize);
    //3. mask parse start
    mRefocusImageInfo.MaskWidth = maskWidth;
    mRefocusImageInfo.MaskHeight = maskHeight;
    mRefocusImageInfo.MaskImageAddr = maskBuffer;
    // mask parse end

    if (createRefocusInstance() && setBufferAddr() && generate()) {
        ALOGI("image refocus init end, success");
        return SUCCESS;
    }
    ALOGI("image refocus init end, fail");
    return FAIL;
}

bool ImageRefocus::initRefocusNoDepthMap(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int imgOrientation, MUINT8* jpsBuffer, int jpsBufferSize, int inStereoImgWidth, int inStereoImgHeight,
        MUINT8* maskBuffer, int maskBufferSize, int maskWidth, int maskHeight) {
    ALOGI("image refocus initRefocusNoDepthMap start inStereoImgWidth %d, inStereoImgHeight %d, outImgWidth %d,"
            "outImgHeight %d", inStereoImgWidth, inStereoImgHeight, outImgWidth, outImgHeight);
    /* Image Information */
    mStereoImgWidth = inStereoImgWidth;
    mSetreoImgHeight = inStereoImgHeight;
    mOutTargetImgWidth = outImgWidth;
    mOutTargetImgHeight = outImgHeight;

    ALOGI("image refocus initRefocusIMGSource ");
    initRefocusIMGSource(sourceFilePath, outImgWidth, outImgHeight, imgOrientation);
    //2. jps parse start
    mRefocusImageInfo.Width = mStereoImgWidth;
    mRefocusImageInfo.Height = mSetreoImgHeight;
    mRefocusImageInfo.ImgAddr[0] = jpsBuffer;

    initJPSBuffer(jpsBuffer, jpsBufferSize);
    //3. mask parse start
    mRefocusImageInfo.MaskWidth = maskWidth;
    mRefocusImageInfo.MaskHeight = maskHeight;
    mRefocusImageInfo.MaskImageAddr = maskBuffer;
    // mask parse end

    if (createRefocusInstance() && setBufferAddr() && generate()) {
        ALOGI("image refocus init end, success");
        return SUCCESS;
    }
    ALOGI("image refocus init end, fail");
    return FAIL;
}

bool ImageRefocus::initRefocusWithDepthMap(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int imgOrientation, MUINT8* depthMapBuffer, int depthBufferSize, int inStereoImgWidth, int inStereoImgHeight) {
    ALOGI("image refocus initRefocusWithDepthMap start outImgWidth %d, outImgHeight %d", outImgWidth, outImgHeight);
    /* Image Information */
    mOutTargetImgWidth = outImgWidth;
    mOutTargetImgHeight = outImgHeight;

    initRefocusIMGSource(sourceFilePath, outImgWidth, outImgHeight, imgOrientation);

    ALOGI("image refocus set depthbuffer info start %d  size %d", depthMapBuffer, depthBufferSize);
    mRefocusImageInfo.Width = inStereoImgWidth;
    mRefocusImageInfo.Height = inStereoImgHeight;
    mRefocusImageInfo.DepthBufferAddr = depthMapBuffer;
    mRefocusImageInfo.DepthBufferSize = depthBufferSize;
    ALOGI("image refocus set depthbuffer info end   outImgHeight %d outImgWidth %d", outImgHeight, outImgWidth);
    p_jpsImgBuffer = NULL;
    //config parse start
    //parse_configuration("/sdcard/Pictures/Config.cfg");

    if (createRefocusInstance() && setBufferAddr() && generate()) {
        ALOGI("image refocus init end, success");
        return SUCCESS;
    }
    ALOGI("image refocus init end, fail");
    return FAIL;
}

void ImageRefocus::initRefocusIMGSource(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int imgOrientation) {
    ALOGI("image refocus initRefocusIMGSource start");
    sprintf(mSourceFileName, "%s", sourceFilePath);

    // assign orientation
    //mRefocusImageInfo.Orientation = REFOCUS_ORIENTATION_0;
    /*memory allocation*/
    MUINT32 TargetSize;

    //mSimRefocusInput.TargetWidth = outImgWidth;
    //mSimRefocusInput.TargetHeight = outImgHeight;

    TargetSize = ALIGN16(outImgWidth) * ALIGN16(outImgHeight) * 3 / 2;
    //unsigned char *p_targetImgBuffer = new unsigned char[TargetSize];
    p_targetImgBuffer = (unsigned char *) malloc(TargetSize);
    ALOGI("image refocus decode image p_targetImgBuffer %d ", p_targetImgBuffer);
    //mSimRefocusInput.TargetImageAddr = (MUINT32)p_targetImgBuffer;

    ALOGI("image refocus decode image resource start w  %d, H %d ", outImgWidth, outImgHeight);
    jpgDecode(sourceFilePath, (uint8_t*) p_targetImgBuffer, outImgWidth, outImgHeight);
    ALOGI("image refocus decode image resource end");

    //for target image
    mRefocusImageInfo.TargetWidth = outImgWidth;
    mRefocusImageInfo.TargetHeight = outImgHeight;
    mRefocusImageInfo.TargetImgAddr = (MUINT8*) p_targetImgBuffer;
    ALOGI("image refocus initRefocusIMGSource end");
}

void ImageRefocus::initJPSIMGSource(const char *jpsFilePath, int jpsImgWidth, int jpsImgHeight, int jpsImgOrientation) {
    ALOGI("image refocus initRefocusIMGSource start");

    /*memory allocation*/
    MUINT32 TargetSize;

    TargetSize = ALIGN16(jpsImgWidth) * ALIGN16(jpsImgHeight) * 3 / 2;
    //unsigned char *p_targetImgBuffer = new unsigned char[TargetSize];
    p_jpsImgBuffer = (unsigned char *) malloc(TargetSize);
    ALOGI("image refocus decode image p_jpsImgBuffer %d ", p_jpsImgBuffer);
    //char sourceTestFilePath[100] = "/storage/sdcard0/DCIM/IMG_20100101_000350_1.jpg";
    jpgDecode(jpsFilePath, (uint8_t*) p_jpsImgBuffer, jpsImgWidth, jpsImgHeight);
    ALOGI("image refocus decode image resource end");

    //for target image
    //mRefocusImageInfo.Width = mSimRefocusInput.Width;
    //mRefocusImageInfo.Height = mSimRefocusInput.Height;
    mRefocusImageInfo.ImgAddr[0] = (MUINT8*) p_jpsImgBuffer;
    ALOGI("image refocus initJPSIMGSource end");
}

void ImageRefocus::initRefocusDepthInfo(const char *depthmapSourcePath, int inStereoImgWidth, int inStereoImgHeight) {
    MUINT32 StereoSize;

    //mSimRefocusInput.StereoWidth = inStereoImgWidth;
    //mSimRefocusInput.StereoHeight = inStereoImgHeight;
    StereoSize = inStereoImgWidth * inStereoImgHeight * 3 / 2;
    //allocate memory for depth info image
    //unsigned char *p_pano3d_buffer = new unsigned char [StereoSize*2];
    unsigned char *p_pano3d_buffer = (unsigned char *) malloc(StereoSize * 2);
    ALOGI("image refocus decode image p_targetImgBuffer %d ", p_pano3d_buffer);
    // store result in MyPano3DResultInfo
    //mSimRefocusInput.LeftImageAddr = (MUINT32)p_pano3d_buffer;
    jpgDecode(depthmapSourcePath, (uint8_t*) p_pano3d_buffer, inStereoImgWidth, inStereoImgHeight);
    ALOGI("image refocus decode depthmap resource end");

    //for stereo image
    mRefocusImageInfo.Width = inStereoImgWidth;
    mRefocusImageInfo.Height = inStereoImgHeight;
    mRefocusImageInfo.ImgAddr[0] = (MUINT8*) p_pano3d_buffer;
}

void ImageRefocus::initJPSBuffer(MUINT8* jpsBuffer, int jpsBufferSize) {
    MUINT32 targetSize;

    unsigned char* pJpsBuffer = (unsigned char *) malloc(ALIGN128(jpsBufferSize) + 512 + 127);
    unsigned char * align128_file_buffer = (unsigned char *) ((((size_t) pJpsBuffer + 127) >> 7) << 7);

    if (pJpsBuffer == NULL) {
        ALOGI("ERROR: pJpsBuffer malloc fail!!!");
        return;
    }
    memcpy(align128_file_buffer, jpsBuffer, jpsBufferSize);

    targetSize = ALIGN16(mRefocusImageInfo.Width) * ALIGN16(mRefocusImageInfo.Height) * 3 / 2;
    p_jpsImgBuffer = (unsigned char *) malloc(targetSize);
    //ALOGI("image refocus decode image p_jpsImgBuffer %d  TargetSize %d ", p_jpsImgBuffer, TargetSize);
    if (!jpgToYV12(align128_file_buffer, jpsBufferSize, (uint8_t*) p_jpsImgBuffer, mRefocusImageInfo.Width,
            (mRefocusImageInfo.Height + 8))) {
        ALOGI("[decodeOneImage]decode failed!!");
        free(pJpsBuffer);
        pJpsBuffer = NULL;
    }

    free(pJpsBuffer);
    pJpsBuffer = NULL;

    mRefocusImageInfo.ImgAddr[0] = (MUINT8*) p_jpsImgBuffer;
}

bool ImageRefocus::createRefocusInstance() {
    ALOGI("image refocus createRefocusInstance start");
    // init
    mRefocusInitInfo.pTuningInfo = &mRefocusTuningInfo;

    getTime(&mStartSec, &mStartNsec);

    mRefocus = mRefocus->createInstance(DRV_REFOCUS_OBJ_SW);

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->createInstance time %10d", mTimeDiff);

    MUINT32 initResult;

    getTime(&mStartSec, &mStartNsec);

    initResult = mRefocus->RefocusInit((MUINT32 *) &mRefocusInitInfo, 0);

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->RefocusInit time %10d", mTimeDiff);

    if (initResult != S_REFOCUS_OK) {
        ALOGI("image refocus createRefocusInstance fail ");
        return FAIL;
    }
    ALOGI("image refocus createRefocusInstance success ");
    return SUCCESS;
}

bool ImageRefocus::setBufferAddr() {
    ALOGI("image refocus setBufferAddr ");
    // get buffer size
    MUINT32 result;
    MUINT32 buffer_size;

    getTime(&mStartSec, &mStartNsec);

    ALOGI("image refocus setBufferAddr mRefocusImageInfo  "
            "TargetWidth %d, TargetHieght %d, TargetImgAddr %d  ImgNum %d, Width %d Height %d ImgAddr %d "
            "DepthBufferAddr %d  DepthBufferSize %d Orientation %d  MainCamPos %d", mRefocusImageInfo.TargetWidth,
            mRefocusImageInfo.TargetHeight, mRefocusImageInfo.TargetImgAddr, mRefocusImageInfo.ImgNum,
            mRefocusImageInfo.Width, mRefocusImageInfo.Height, mRefocusImageInfo.ImgAddr,
            mRefocusImageInfo.DepthBufferAddr, mRefocusImageInfo.DepthBufferSize, mRefocusImageInfo.JPSOrientation,
            mRefocusImageInfo.MainCamPos);

    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_WORKBUF_SIZE, (void *) &mRefocusImageInfo,
            (void *) &buffer_size);

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->get_workbuff_size time %10d", mTimeDiff);

    ALOGI("image refocus setBufferAddr REFOCUS_FEATURE_GET_WORKBUF_SIZE buffer size  %d, result %d ", buffer_size,
            result);
    if (result != S_REFOCUS_OK) {
        ALOGI("image refocus GET_WORKBUF_SIZE fail ");
        return FAIL;
    }

    // set buffer address
    //unsigned char *pWorkingBuffer = new unsigned char[buffer_size];
    pWorkingBuffer = (unsigned char *) malloc(buffer_size);
    mRefocusInitInfo.WorkingBuffAddr = (MUINT8*) pWorkingBuffer;

    getTime(&mStartSec, &mStartNsec);

    ALOGI("image refocus setBufferAddr SET_WORKBUF_ADDR start");
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_SET_WORKBUF_ADDR, (void *) &mRefocusInitInfo.WorkingBuffAddr,
            NULL);

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->set_workbuff_size time %10d", mTimeDiff);

    if (result != S_REFOCUS_OK) {
        ALOGI("image refocus SET_WORKBUF_ADDR fail ");
        return FAIL;
    }
    ALOGI("image refocus SET_WORKBUF_ADDR success ");
    return SUCCESS;
}

bool ImageRefocus::generate() {
    MUINT32 result;
    // algorithm - gen depth map
    ALOGI("image refocus generate start");
    ALOGI("mRefocusImageInfo.RcfyError %d, ", mRefocusImageInfo.RcfyError);
    ALOGI("mRefocusImageInfo.JPSOrientation %d, ", mRefocusImageInfo.JPSOrientation);

    getTime(&mStartSec, &mStartNsec);

    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_ADD_IMG, (void *) &mRefocusImageInfo, NULL);

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->add_image time %10d", mTimeDiff);

    ALOGI("image refocus get result add image  %d ", result);
    if (result != S_REFOCUS_OK) {
        ALOGI("image refocus ADD_IMG fail ");
        return FAIL;
    }

    getTime(&mStartSec, &mStartNsec);

    result = mRefocus->RefocusMain();

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->RefocusMain time %10d", mTimeDiff);

    ALOGI("image refocus get result mainResult %d ", result);
    if (result != S_REFOCUS_OK) {
        ALOGI("image refocus RefocusMain fail ");
        return FAIL;
    }
    getTime(&mStartSec, &mStartNsec);

    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_RESULT, NULL, (void *) &mRefocusResultInfo);

    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->get_result time %10d", mTimeDiff);

    ALOGI("image refocus get result get result %d ", result);
    ALOGI("image refocus generate w  %d  h  %d", mRefocusResultInfo.RefocusImageWidth,
            mRefocusResultInfo.RefocusImageHeight);
    ALOGI("image refocus generate DepthBufferWidth  %d  DepthBufferHeight  %d", mRefocusResultInfo.DepthBufferWidth,
            mRefocusResultInfo.DepthBufferHeight);
    ALOGI("<generate> DepthBufferSize:%d,DepthBufferAddr:%d", mRefocusResultInfo.DepthBufferSize,
            mRefocusImageInfo.DepthBufferAddr);
    if (result != S_REFOCUS_OK) {
        ALOGI("image refocus GET_RESULT fail ");
        return FAIL;
    }
    if (mRefocusImageInfo.DepthBufferAddr == NULL) {
        MUINT8* depthBuffer = new MUINT8[mRefocusResultInfo.DepthBufferSize];
        memcpy(depthBuffer, mRefocusResultInfo.DepthBufferAddr, mRefocusResultInfo.DepthBufferSize);
        mRefocusImageInfo.DepthBufferAddr = depthBuffer;
        mRefocusImageInfo.DepthBufferSize = mRefocusResultInfo.DepthBufferSize;
        ALOGI("image refocus copy depthBuffer from %d to %d", mRefocusResultInfo.DepthBufferAddr, depthBuffer);
    }
    //#if defined(DUMP_REFOCUS_IMAGE)
    if (isDumpRefocusYUVImage) {
        saveRefocusResult(&mRefocusResultInfo, &mRefocusImageInfo);
    }
    //#endif
    return SUCCESS;
}

bool ImageRefocus::generateRefocusImage(MUINT8* array, int touchCoordX, int touchCoordY, int depthOfField) {
    ALOGI("image refocus generateRefocusImage touchCoordX %d touchCoordY %d, depthOfField %d ", touchCoordX,
            touchCoordY, depthOfField);
    mRefocusImageInfo.TouchCoordX = touchCoordX;
    mRefocusImageInfo.TouchCoordY = touchCoordY;
    mRefocusImageInfo.DepthOfField = depthOfField;
    bool generateResult;
    generateResult = generate();
    if (!generateResult)
        return FAIL;

    getTime(&mStartSec, &mStartNsec);
    memcpy(array, (MUINT8*) mRefocusResultInfo.RefocusedRGBAImageAddr,
            mRefocusResultInfo.RefocusImageWidth * mRefocusResultInfo.RefocusImageHeight * 4);
    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance generateRefocusImage memcpy time %10d", mTimeDiff);

    ALOGI("image refocus memcpy done");
    if (mRefocusResultInfo.RefocusImageWidth == 0 || mRefocusResultInfo.RefocusImageHeight == 0) {
        return FAIL;
    }
    //#if defined(DUMP_REFOCUS_RGB_IMAGE)
    if (isDumpRefocusRGBImage) {
        char file_dumpbuffer[FILE_NAME_LENGTH];
        sprintf(file_dumpbuffer, "%s_%s_%d_%d_%d.raw", mSourceFileName, "_rgb_buffer", touchCoordX, touchCoordY,
                depthOfField);
        dumpBufferToFile((MUINT8*) mRefocusResultInfo.RefocusedRGBAImageAddr,
                mRefocusResultInfo.RefocusImageWidth * mRefocusResultInfo.RefocusImageHeight * 4, file_dumpbuffer);
    }
    //#endif

    return SUCCESS;
}

void ImageRefocus::deinit() {
    // uninit
    ALOGI("image refocus deinit start");
    if (NULL != pWorkingBuffer) {
        free(pWorkingBuffer);
        pWorkingBuffer = NULL;
    }
    ALOGI("image refocus deinit free pWorkingBuffer");
    if (NULL != p_targetImgBuffer) {
        free(p_targetImgBuffer);
        p_targetImgBuffer = NULL;
    }
    ALOGI("image refocus deinit free p_targetImgBuffer %d ", p_jpsImgBuffer);
    if (NULL != p_jpsImgBuffer) {
        free(p_jpsImgBuffer);
        p_jpsImgBuffer = NULL;
    }
    ALOGI("image refocus deinit free p_jpsImgBuffer");
    mRefocus->RefocusReset();
    ALOGI("image refocus deinit RefocusReset");
    mRefocus->destroyInstance(mRefocus);
}

int ImageRefocus::getDepthBufferSize() {
    return mRefocusResultInfo.DepthBufferSize;
}

int ImageRefocus::getDepthBufferWidth() {
    return mRefocusResultInfo.DepthBufferWidth;
}

int ImageRefocus::getDepthBufferHeight() {
    return mRefocusResultInfo.DepthBufferHeight;
}

int ImageRefocus::getXMPDepthBufferSize() {
    return mRefocusResultInfo.XMPDepthWidth * mRefocusResultInfo.XMPDepthHeight;
}

int ImageRefocus::getXMPDepthBufferWidth() {
    return mRefocusResultInfo.XMPDepthWidth;
}

int ImageRefocus::getXMPDepthBufferHeight() {
    return mRefocusResultInfo.XMPDepthHeight;
}

int ImageRefocus::getMetaBufferWidth() {
    return mRefocusResultInfo.MetaBufferWidth;
}
int ImageRefocus::getMetaBufferHeight() {
    return mRefocusResultInfo.MetaBufferHeight;
}

void ImageRefocus::saveDepthMapInfo(MUINT8* depthBufferArray, MUINT8* xmpDepthBufferArray) {
    ALOGI("saveDepthMapInfo DepthBufferSize %d ", mRefocusResultInfo.DepthBufferSize);
    memcpy(depthBufferArray, (MUINT8*) mRefocusResultInfo.DepthBufferAddr, mRefocusResultInfo.DepthBufferSize);
    memcpy(xmpDepthBufferArray, (MUINT8*) mRefocusResultInfo.XMPDepthMapAddr,
            mRefocusResultInfo.XMPDepthWidth * mRefocusResultInfo.XMPDepthHeight);
}

void ImageRefocus::saveRefocusImage(const char *saveFileName, int inSampleSize) {
    mRefocusImageInfo.TouchCoordX = mRefocusImageInfo.TouchCoordX * inSampleSize;
    mRefocusImageInfo.TouchCoordY = mRefocusImageInfo.TouchCoordY * inSampleSize;
    getTime(&mStartSec, &mStartNsec);
    initRefocusIMGSource(mSourceFileName, mRefocusImageInfo.TargetWidth * inSampleSize,
            mRefocusImageInfo.TargetHeight * inSampleSize, 0);
    if (createRefocusInstance() && setBufferAddr() && generate()) {
        ALOGI("image refocus init end, success");
        //return SUCCESS;
    }
    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->saveRefocusImage generate time %10d", mTimeDiff);

    char file[FILE_NAME_LENGTH];
    sprintf(file, "%s", saveFileName);
    FILE *fp;
    unsigned char *src_va = NULL;
    ALOGI("test file:%s", file);
    fp = fopen(file, "w");
    if (fp == NULL) {
        ALOGI("[ULT]ERROR: Open file %s failed.", file);
        return;
    }

    //should free this memory when not use it !!!
    src_va = (unsigned char *) malloc(mRefocusResultInfo.RefocusImageWidth * mRefocusResultInfo.RefocusImageHeight);
    ALOGI("src va :%p", src_va);

    if (src_va == NULL) {
        ALOGI("Can not allocate memory");
        fclose(fp);
        return;
    }

    ALOGI("src va :%p", mRefocusResultInfo.RefocusedYUVImageAddr);

    getTime(&mStartSec, &mStartNsec);
    yv12ToJpg((unsigned char *) mRefocusResultInfo.RefocusedYUVImageAddr,
            mRefocusResultInfo.RefocusImageWidth * mRefocusResultInfo.RefocusImageHeight,
            mRefocusResultInfo.RefocusImageWidth, mRefocusResultInfo.RefocusImageHeight, src_va,
            mRefocusResultInfo.RefocusImageWidth * mRefocusResultInfo.RefocusImageHeight, mJpgSize);
    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("performance mRefocus->saveRefocusImage yv12ToJpg time %10d", mTimeDiff);
    dumpBufferToFile(src_va, mJpgSize, file);
    free(src_va);
    fclose(fp);
}

ImageRefocus::~ImageRefocus() {
    if (mRefocusImageInfo.DepthBufferAddr != NULL) {
        delete mRefocusImageInfo.DepthBufferAddr;
    }
    ALOGI("end");
}

bool ImageRefocus::jpgDecode(char const *fileName, uint8_t *dstBuffer, uint32_t dstWidth, uint32_t dstHeight) {
    MUINT32 file_size;
    unsigned char *file_buffer;
    unsigned char *align128_file_buffer;
    FILE *fp;
    MUINT32 ret;

    ALOGI("begin");
    //open a image file
    fp = fopen(fileName, "rb");
    if (fp == NULL) {
        ALOGI("[readImageFile]ERROR: Open file %s failed.", fileName);
        return false;
    }
    ALOGI("open file %s success!", fileName);
    //get file size
    fseek(fp, SEEK_SET, SEEK_END);
    file_size = ftell(fp);
    ALOGI("[decodeOneImage]file_size is %d", file_size);

    if (file_size == 0) {
        ALOGI("ERROR: [readImageFile]file size is 0");
        fclose(fp);
        return false;
    }

    //allocate buffer for the file
    //should free this memory when not use it !!!
    file_buffer = (unsigned char *) malloc(ALIGN128(file_size) + 512 + 127);
    align128_file_buffer = (unsigned char *) ((((size_t) file_buffer + 127) >> 7) << 7);
    ALOGI("src va :%p", align128_file_buffer);
    ALOGI("[decodeOneImage]memory 128 alignment");
    if (align128_file_buffer == NULL) {
        ALOGI("Can not allocate memory");
        fclose(fp);
        return false;
    }

    //read image file
    fseek(fp, SEEK_SET, SEEK_SET);
    ret = fread(align128_file_buffer, 1, file_size, fp);
    if (ret != file_size) {
        ALOGI("File read error ret[%d]", ret);
        fclose(fp);
        return false;
    }
    ALOGI("read file to buffer success!");
#ifdef MM_PROFILING
    getTime(&mStartSec, &mStartNsec);
#endif

    ALOGI("jpgDecode srcSize %d ", file_size);
    if (!jpgToYV12(align128_file_buffer, file_size, dstBuffer, dstWidth, dstHeight)) {
        ALOGI("[decodeOneImage]decode failed!!");
    }

    //#ifdef DUMP_DECODE_IMAGE
    if (isDumpDecodeImage) {
        char jpgname[FILE_NAME_LENGTH];
        sprintf(jpgname, "%s_%d.bmp", fileName, "_decode");
        unsigned int dstFormat = FORMAT_YV12;
    }
    //#endif

#ifdef MM_PROFILING
    getTime(&mEndSec, &mEndNsec);
    mTimeDiff = getTimeDiff(mStartSec, mStartNsec, mEndSec, mEndNsec);
    ALOGI("%10d ==> jpgToYV12: jpg to yv12", mTimeDiff);
#endif
    // release file buffer
    free(file_buffer);

    //close image file
    fclose(fp);
    ALOGI("end");
    return true;
}

bool ImageRefocus::jpgToYV12(uint8_t* srcBuffer, uint32_t srcSize, uint8_t *dstBuffer, uint32_t dstWidth,
        uint32_t dstHeight) {
    MHAL_JPEG_DEC_INFO_OUT outInfo;
    MHAL_JPEG_DEC_START_IN inParams;
    MHAL_JPEG_DEC_SRC_IN srcInfo;
    void *fSkJpegDecHandle;
    unsigned int cinfo_output_width, cinfo_output_height;
    int re_sampleSize;
    //int preferSize = 0;
    // TODO: samplesize value
    //int sampleSize = 8;
    int width, height;

    ALOGI("onDecode start %d ", srcSize);
    //2 step1: set sampleSize value
    //sampleSize = roundToTwoPower(sampleSize);
    //2 step2: init fSkJpegDecHandle
    fSkJpegDecHandle = srcInfo.jpgDecHandle = NULL;
    //2 step3: init  inparam
    //memcpy(&inParams, param, sizeof(MHAL_JPEG_DEC_START_IN));
    inParams.dstFormat = (JPEG_OUT_FORMAT_ENUM) FORMAT_YV12;
    inParams.srcBuffer = srcBuffer;
    inParams.srcBufSize = ALIGN512(srcSize);
    inParams.srcLength = srcSize;
    inParams.dstWidth = dstWidth;
    inParams.dstHeight = dstHeight;
    inParams.dstVirAddr = dstBuffer;
    inParams.dstPhysAddr = NULL;
    inParams.doDithering = 0;
    inParams.doRangeDecode = 0;
    inParams.doPostProcessing = 0;
    inParams.postProcessingParam = NULL;
    inParams.PreferQualityOverSpeed = 0;

    //2 step4: init srcInfo
    srcInfo.srcBuffer = srcBuffer;
    srcInfo.srcLength = srcSize;
    //2 step5 jpeg dec parser
    ALOGI("onDecode MHAL_IOCTL_JPEG_DEC_PARSER");
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_PARSER, (void *) &srcInfo, sizeof(srcInfo), NULL, 0, NULL)) {
        ALOGI("[onDecode]parser file error");
        return false;
    }
    //2 step6 set jpgDecHandle value
    outInfo.jpgDecHandle = srcInfo.jpgDecHandle;
    ALOGI("outInfo.jpgDecHandle --> %d", outInfo.jpgDecHandle);
    //2 step7: get jpeg info
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_GET_INFO, NULL, 0, (void *) &outInfo, sizeof(outInfo), NULL)) {
        ALOGI("[onDecode]get info error");
        return false;
    }
    ALOGI("outInfo.srcWidth --> %d", outInfo.srcWidth);
    ALOGI("outInfo.srcHeight -- > %d", outInfo.srcHeight);

    //2 step8: set inParams
    inParams.jpgDecHandle = srcInfo.jpgDecHandle;
    inParams.dstWidth = ALIGN16(inParams.dstWidth);
    inParams.dstHeight = ALIGN16(inParams.dstHeight);

    ALOGI("inParams.dstFormat --> %d", inParams.dstFormat);
    ALOGI("inParams.dstWidth -- > %d", inParams.dstWidth);
    ALOGI("inParams.dstHeight --> %d", inParams.dstHeight);
    ALOGI("inParams.srcLength --> %d", inParams.srcLength);
    //2 step9: start decode
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_START, (void *) &inParams, sizeof(inParams), NULL, 0, NULL)) {
        ALOGI("JPEG HW not support this image");
        return false;
    }

    return true;
}

int ImageRefocus::roundToTwoPower(int a) {
    int ans = 1;

    if (a >= 8)
        return a;

    while (a > 0) {
        a = a >> 1;
        ans *= 2;
    }

    return (ans >> 1);
}

void ImageRefocus::dumpBufferToFile(MUINT8* buffer, int bufferSize, char* fileName) {
    FILE* fp;
    int index;

    ALOGI("dump buffer to file buffer address:0x%x", buffer);

    if (buffer == NULL)
        return;
    ALOGI("dump buffer to file:%s", fileName);

    fp = fopen(fileName, "w");
    if (fp == NULL) {
        ALOGI("ERROR: Open file %s failed.", fileName);
        return;
    }

    ALOGI("bufferSize %d ", bufferSize);

    for (index = 0; index < bufferSize; index++) {
        fprintf(fp, "%c", buffer[index]);
    }
    ALOGI("dump buffer to file success!");
    fclose(fp);
}

unsigned int ImageRefocus::getDstSize(unsigned int width, unsigned int height, int fmt) {
    unsigned int size;
    switch (fmt) {
    case JPEG_OUT_FORMAT_RGB565: //JpgDecHal::kRGB_565_Format:
        size = width * height * 2;
        break;
    case JPEG_OUT_FORMAT_RGB888: //JpgDecHal::kRGB_888_Format:
        size = width * height * 3;
        break;
    case JPEG_OUT_FORMAT_ARGB8888: //JpgDecHal::kARGB_8888_Format:
        size = width * height * 4;
        break;
    case 5:
        size = width * height * 3 / 2;
        break;
    default:
        size = 0;
        break;
    }
    return size;
}

void ImageRefocus::saveRefocusResult(RefocusResultInfo* pResultInfo, RefocusImageInfo* pImageInfo) {
    BITMAP bmp;
    char filename[FILE_NAME_LENGTH];

    sprintf(filename, "%s_%d_%d_%d.bmp", mSourceFileName, pImageInfo->TouchCoordX, pImageInfo->TouchCoordY,
            pImageInfo->DepthOfField);
    ALOGI("filename  %s", filename);
    UTIL_BASE_IMAGE_STRUCT img;
    char *ImageBuffer = new char[pResultInfo->RefocusImageWidth * pResultInfo->RefocusImageHeight * 4];
    bmp_create(&bmp, pResultInfo->RefocusImageWidth, pResultInfo->RefocusImageHeight, 0);
    img.data = (MUINT32 *) (pResultInfo->RefocusedYUVImageAddr);
    img.width = pResultInfo->RefocusImageWidth;
    img.height = pResultInfo->RefocusImageHeight;
    YUV420_toBMP((kal_uint8 *) img.data, &bmp);
    bmp_write(filename, &bmp);
    bmp_free(&bmp);
}

bool ImageRefocus::yv12ToJpg(unsigned char *srcBuffer, int srcSize, int srcWidth, int srcHeight,
        unsigned char *dstBuffer, int dstSize, MUINT32 &u4EncSize) {
    MBOOL ret = true;
    int fIsAddSOI = true; //if set true, not need add exif
    int quality = 90;

    size_t yuvAddr[3], yuvSize[3];

    yuvSize[0] = getBufSize(srcWidth, srcHeight, YUV_IMG_STRIDE_Y);
    yuvSize[1] = getBufSize(srcWidth / 2, srcHeight / 2, YUV_IMG_STRIDE_U);
    yuvSize[2] = getBufSize(srcWidth / 2, srcHeight / 2, YUV_IMG_STRIDE_V);
    //
    yuvAddr[0] = (size_t) srcBuffer;
    yuvAddr[1] = yuvAddr[0] + yuvSize[0];
    yuvAddr[2] = yuvAddr[1] + yuvSize[1];

    //
    // (0). debug
    ALOGI("begin");
    ALOGI("Y tride:%d, U tride:%d, V tride:%d", YUV_IMG_STRIDE_Y, YUV_IMG_STRIDE_U, YUV_IMG_STRIDE_V);
    ALOGI("srcBuffer=%p", srcBuffer);
    ALOGI("dstBuffer=%p", dstBuffer);
    ALOGI("width=%d", srcWidth);
    ALOGI("height=%d", srcHeight);

    ALOGI("yuvSize[0]=0x%x", yuvSize[0]);
    ALOGI("yuvSize[1]=0x%x", yuvSize[1]);
    ALOGI("yuvSize[2]=0x%x", yuvSize[2]);

    ALOGI("yuvAddr[0]=0x%x", yuvAddr[0]);
    ALOGI("yuvAddr[1]=0x%x", yuvAddr[1]);
    ALOGI("yuvAddr[2]=0x%x", yuvAddr[2]);

    // (1). Create Instance
    JpgEncHal* pJpgEncoder = new JpgEncHal();

    // (1). Lock
    pJpgEncoder->unlock();
    if (!pJpgEncoder->lock()) {
        ALOGI("can't lock jpeg resource");
        goto EXIT;
    }

    // (2). size, format, addr
    ALOGI("jpeg source YV12");
    pJpgEncoder->setEncSize(srcWidth, srcHeight, JpgEncHal::kENC_YV12_Format); //JpgEncHal:: kENC_NV21_Format);

    ALOGI("setSrcAddr");

    pJpgEncoder->setSrcAddr((void*) ALIGN16(yuvAddr[0]), (void*) ALIGN16(yuvAddr[1]), (void*) ALIGN16(yuvAddr[2]));
    ALOGI("setSrcBufSize");
    pJpgEncoder->setSrcBufSize(srcWidth, yuvSize[0], yuvSize[1], yuvSize[2]);
    // (3). set quality
    ALOGI("setQuality");
    pJpgEncoder->setQuality(quality);
    // (4). dst addr, size
    ALOGI("setDstAddr");
    pJpgEncoder->setDstAddr((void *) dstBuffer);
    ALOGI("setDstSize");
    pJpgEncoder->setDstSize(dstSize);
    // (6). set SOI
    ALOGI("enableSOI");
    pJpgEncoder->enableSOI((fIsAddSOI > 0) ? 1 : 0);
    // (7). ION mode
    ALOGI("start");
    // (8).  Start
    if (pJpgEncoder->start(&u4EncSize)) {
        //add head
        //dstBuffer[0] = 0xff;
        //dstBuffer[1] = 0xD8;
        ALOGI("Jpeg encode done, size = %d", u4EncSize);
        ret = true;
        ALOGI("encode success");
    } else {
        ALOGI("encode fail");
        pJpgEncoder->unlock();
        goto EXIT;
    }

    pJpgEncoder->unlock();

    EXIT: delete pJpgEncoder;
    ALOGI("end ret:%d", ret);
    return ret;
}

MUINT32 ImageRefocus::getBufSize(MUINT32 width, MUINT32 height, MUINT32 stride) {
    MUINT32 bufSize = 0;
    MUINT32 w;

    w = ALIGN16(width);
    bufSize = w * height;
    ALOGI("W(%d)xH(%d),BS(%d)", w, height, bufSize);

    return bufSize;
}

void ImageRefocus::getTime(int *sec, int *usec) {
    timeval time;
    gettimeofday(&time, NULL);
    *sec = time.tv_sec;
    *usec = time.tv_usec;
}

int ImageRefocus::getTimeDiff(int startSec, int startNSec, int endSec, int endNSec) {
    return ((endSec - startSec) * 1000000 + (endNSec - startNSec)) / 1000;
}

void ImageRefocus::debugConfig() {
    FILE *fpYUVDump;
    FILE *fpRGBDump;
    FILE *fpDecodeDump;
    fpYUVDump = fopen(dumpRefocusYUVImageConfig, "r");
    if (fpYUVDump == NULL) {
        ALOGI("dump refocus yuv image off");
        isDumpRefocusYUVImage = false;
    } else {
        isDumpRefocusYUVImage = true;
        fclose(fpYUVDump);
    }
    fpRGBDump = fopen(dumpRefocusRGBImageConfig, "r");
    if (fpRGBDump == NULL) {
        ALOGI("dump refocus rgb image off");
        isDumpRefocusRGBImage = false;
    } else {
        isDumpRefocusRGBImage = true;
        fclose(fpRGBDump);
    }
    fpDecodeDump = fopen(dumpDecodeImageConfig, "r");
    if (fpDecodeDump == NULL) {
        ALOGI("dump refocus decode image off");
        isDumpDecodeImage = false;
    } else {
        isDumpDecodeImage = true;
        fclose(fpDecodeDump);
    }
}

// for debug information
void ImageRefocus::parse_configuration(char* configFilename) {
    int maxline = 200;
    char ident[200];
    char oneline[200], *token;
    char seps = '=';
    FILE* config_fp = 0;

    ident[maxline - 1] = 0;

    if ((config_fp = fopen(configFilename, "r")) == NULL) {
        printf("The configuration file cannot be opened.\n");
        return;
    }

    while (!feof(config_fp)) {
        fgets(oneline, maxline, config_fp);
        first_arg(oneline, ident); // Get the first argument
        switch (ident[0]) {
        case '#': // comment
            continue;
            break;

        default: // words
            token = (char *) strchr(oneline, seps);
            if (!token)
                break;

            strcpy(oneline, token + 2);

            if (!strcmp("C_ITERATION", ident)) {
                sscanf(oneline, "%d", &mRefocusTuningInfo.IterationTimes);
                ALOGI("C_ITERATION     =%d\n", mRefocusTuningInfo.IterationTimes);
            }

            if (!strcmp("DS_H", ident)) {
                sscanf(oneline, "%d", &mRefocusTuningInfo.HorzDownSampleRatio);
                ALOGI("DS_H            =%d\n", mRefocusTuningInfo.HorzDownSampleRatio);
            }

            if (!strcmp("DS_V", ident)) {
                sscanf(oneline, "%d", &mRefocusTuningInfo.VertDownSampleRatio);
                ALOGI("DS_V            =%d\n", mRefocusTuningInfo.VertDownSampleRatio);
            }

            if (!strcmp("DRZ_WD", ident)) {
                sscanf(oneline, "%d", &mRefocusImageInfo.DRZ_WD);
                ALOGI("DRZ_WD        =%d\n", mRefocusImageInfo.DRZ_WD);
            }

            if (!strcmp("DRZ_HT", ident)) {
                sscanf(oneline, "%d", &mRefocusImageInfo.DRZ_HT);
                ALOGI("DRZ_HT        =%d\n", mRefocusImageInfo.DRZ_HT);
            }
            break;
        }
    }

    fclose(config_fp);

    return;
}

char* ImageRefocus::first_arg(char *argument, char *arg_first) {
    memset(arg_first, 0, strlen(arg_first));

    while (*argument == ' ')
        argument++;

    while (*argument != '\0') {
        if (*argument == ' ' || *argument == '=' || *argument == '\t')
            break;
        sprintf(arg_first, "%s%c", arg_first, (char) *argument);
        argument++;
    }

    while (*argument == ' ')
        argument++;

    return argument;
}
}

