#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <random>
#include <android/log.h>
#include <sys/time.h>

#include "SKP_Silk_SDK_API.h"
#include "cqcode.h"

/* Define codec specific settings */
#define MAX_BYTES_PER_FRAME     250 // Equals peak bitrate of 100 kbps
#define MAX_INPUT_FRAMES        5
#define FRAME_LENGTH_MS         20
#define MAX_API_FS_KHZ          48

int silk_encode(int rate, char type, const char* inputFile, const char* outPutFile);

extern "C"
JNIEXPORT jstring JNICALL
Java_moe_fuqiuluo_xposed_actions_impl_PullConfig_testNativeLibrary(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("加载Shamrock库成功~");
}

extern "C"
JNIEXPORT jint JNICALL
Java_moe_fuqiuluo_http_action_helper_codec_AudioUtils_pcmToSilk(JNIEnv *env, jobject thiz,
                                                                jint rate,
                                                                jbyte type,
                                                                jstring pcm_file,
                                                                jstring silk_file) {
    auto pcm = env->GetStringUTFChars(pcm_file, nullptr);
    auto silk = env->GetStringUTFChars(silk_file, nullptr);
    return silk_encode(rate, type, pcm, silk);
}

int silk_encode(int rate, char type, const char* inputFile, const char* outPutFile) {
    double    filetime;
    size_t    counter;
    SKP_int32 k, totPackets, totActPackets, ret;
    SKP_int16 nBytes;
    double    sumBytes, sumActBytes, nrg;
    SKP_uint8 payload[ MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES ];
    SKP_int16 in[ FRAME_LENGTH_MS * MAX_API_FS_KHZ * MAX_INPUT_FRAMES ];
    char      speechInFileName[ 150 ], bitOutFileName[ 150 ];
    FILE      *bitOutFile, *speechInFile;
    SKP_int32 encSizeBytes;
    void      *psEnc;

#ifdef _SYSTEM_IS_BIG_ENDIAN
    SKP_int16 nBytes_LE;
#endif

    /* default settings */
    SKP_int32 API_fs_Hz = rate;
    SKP_int32 max_internal_fs_Hz;
    SKP_int32 targetRate_bps = 25000;
    SKP_int32 smplsSinceLastPacket, packetSize_ms = 20;
    SKP_int32 frameSizeReadFromFile_ms = 20;
    SKP_int32 packetLoss_perc = 0;
#if LOW_COMPLEXITY_ONLY
    SKP_int32 complexity_mode = 0;
#else
    SKP_int32 complexity_mode = 2;
#endif

    SKP_int32 DTX_enabled = 0, INBandFEC_enabled = 0;
    SKP_SILK_SDK_EncControlStruct encControl; // Struct for input to encoder
    SKP_SILK_SDK_EncControlStruct encStatus;  // Struct for status of encoder

    /* get arguments */
    strcpy( speechInFileName, inputFile );
    strcpy( bitOutFileName,   outPutFile );

    /* If no max internal is specified, set to minimum of API fs and 24 kHz */
    max_internal_fs_Hz = rate;

    /* Open files */
    speechInFile = fopen( speechInFileName, "rb" );
    if( speechInFile == nullptr ) {
        return -1;
    }
    bitOutFile = fopen( bitOutFileName, "wb" );
    if( bitOutFile == nullptr ) {
        return -2;
    }

    /* Add Silk header to stream */
    {
        char Tencent_break[1];
        Tencent_break[0] = type;
        fwrite( Tencent_break, sizeof( char ), 1, bitOutFile );

        static const char Silk_header[] = "#!SILK_V3";
        fwrite( Silk_header, sizeof( char ), strlen( Silk_header ), bitOutFile );
    }

    /* Create Encoder */
    ret = SKP_Silk_SDK_Get_Encoder_Size( &encSizeBytes );
    if( ret ) {
        __android_log_print(ANDROID_LOG_ERROR, "SilkCodec", "Error: SKP_Silk_create_encoder returned %d", ret );
        return  -3;
    }

    psEnc = malloc( encSizeBytes );

    /* Reset Encoder */
    ret = SKP_Silk_SDK_InitEncoder( psEnc, &encStatus );
    if( ret ) {
        __android_log_print(ANDROID_LOG_ERROR, "SilkCodec", "Error: SKP_Silk_reset_encoder returned %d", ret );
        return -4;
    }

    /* Set Encoder parameters */
    encControl.API_sampleRate        = API_fs_Hz;
    encControl.maxInternalSampleRate = max_internal_fs_Hz;
    encControl.packetSize            = ( packetSize_ms * API_fs_Hz ) / 1000;
    encControl.packetLossPercentage  = packetLoss_perc;
    encControl.useInBandFEC          = INBandFEC_enabled;
    encControl.useDTX                = DTX_enabled;
    encControl.complexity            = complexity_mode;
    encControl.bitRate               = targetRate_bps;

    totPackets           = 0;
    totActPackets        = 0;
    smplsSinceLastPacket = 0;
    sumBytes             = 0.0;
    sumActBytes          = 0.0;
    smplsSinceLastPacket = 0;

    while( true ) {
        /* Read input from file */
        counter = fread( in, sizeof( SKP_int16 ), ( frameSizeReadFromFile_ms * API_fs_Hz ) / 1000, speechInFile );
#ifdef _SYSTEM_IS_BIG_ENDIAN
        swap_endian( in, counter );
#endif
        if( ( SKP_int )counter < ( ( frameSizeReadFromFile_ms * API_fs_Hz ) / 1000 ) ) {
            break;
        }


        /* max payload size */
        nBytes = MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES;

        /* Silk Encoder */
        ret = SKP_Silk_SDK_Encode( psEnc, &encControl, in, (SKP_int16)counter, payload, &nBytes );
        if( ret ) {
            __android_log_print(ANDROID_LOG_ERROR, "SilkCodec",  "SKP_Silk_Encode returned %d", ret );
        }

        /* Get packet size */
        packetSize_ms = ( SKP_int )( ( 1000 * ( SKP_int32 )encControl.packetSize ) / encControl.API_sampleRate );
        smplsSinceLastPacket += ( SKP_int )counter;
        if( ( ( 1000 * smplsSinceLastPacket ) / API_fs_Hz ) == packetSize_ms ) {
            /* Sends a dummy zero size packet in case of DTX period  */
            /* to make it work with the decoder test program.        */
            /* In practice should be handled by RTP sequence numbers */
            totPackets++;
            sumBytes  += nBytes;
            nrg = 0.0;
            for( k = 0; k < ( SKP_int )counter; k++ ) {
                nrg += in[ k ] * (double)in[ k ];
            }
            if( ( nrg / ( SKP_int )counter ) > 1e3 ) {
                sumActBytes += nBytes;
                totActPackets++;
            }
            /* Write payload size */
#ifdef _SYSTEM_IS_BIG_ENDIAN
            nBytes_LE = nBytes;
            swap_endian( &nBytes_LE, 1 );
            fwrite( &nBytes_LE, sizeof( SKP_int16 ), 1, bitOutFile );
#else
            fwrite( &nBytes, sizeof( SKP_int16 ), 1, bitOutFile );
#endif
            /* Write payload */
            fwrite( payload, sizeof( SKP_uint8 ), nBytes, bitOutFile );
            smplsSinceLastPacket = 0;
            //fprintf(stderr, "\rPackets encoded:                %d", totPackets);
        }

    }

    /* Write dummy because it can not end with 0 bytes */
    nBytes = -1;

    /* Write payload size */
    fwrite( &nBytes, sizeof( SKP_int16 ), 1, bitOutFile );

    /* Free Encoder */
    free( psEnc );

    fclose( speechInFile );
    fclose( bitOutFile );

    filetime  = totPackets * 1e-3 * packetSize_ms;
    return (int) filetime;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_moe_fuqiuluo_http_action_helper_MessageHelper_createMessageUniseq(JNIEnv *env, jobject thiz,
                                                                       jint chat_type,
                                                                       jlong time) {
    static std::random_device rd;
    static std::mt19937 generator(rd());
    static std::uniform_int_distribution<int> distribution(INT32_MIN, INT32_MAX);
    int64_t uniseq = (time / 1000) << (8 * 4);
    auto msgtype = (int64_t) chat_type;
    int64_t random = abs(distribution(generator)) & 0xffffff00L;
    return uniseq | random | msgtype;
}


extern "C"
JNIEXPORT jint JNICALL
Java_moe_fuqiuluo_http_action_helper_MessageHelper_getChatType(JNIEnv *env, jobject thiz,
                                                               jlong msg_id) {
    return (int32_t) ((int64_t) msg_id & 0xffL);
}


extern "C"
JNIEXPORT jobject JNICALL
Java_moe_fuqiuluo_http_action_helper_MessageHelper_nativeDecodeCQCode(JNIEnv *env, jobject thiz,
                                                                jstring code) {
    jclass ArrayList = env->FindClass("java/util/ArrayList");
    jmethodID NewArrayList = env->GetMethodID(ArrayList, "<init>", "()V");
    jmethodID ArrayListAdd = env->GetMethodID(ArrayList, "add", "(Ljava/lang/Object;)Z");
    jobject arrayList = env->NewObject(ArrayList, NewArrayList);

    const char* cCode = env->GetStringUTFChars(code, nullptr);
    std::string cppCode = cCode;
    std::vector<std::unordered_map<std::string, std::string>> dest;
    try {
        decode_cqcode(cppCode, dest);
    } catch (illegal_code& code) {
        return arrayList;
    }

    jclass HashMap = env->FindClass("java/util/HashMap");
    jmethodID NewHashMap = env->GetMethodID(HashMap, "<init>", "()V");
    jclass String = env->FindClass("java/lang/String");
    jmethodID NewString = env->GetMethodID(String, "<init>", "([BLjava/lang/String;)V");
    jstring charset = env->NewStringUTF("UTF-8");
    jmethodID put = env->GetMethodID(HashMap, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    for (auto& map : dest) {
        jobject hashMap = env->NewObject(HashMap, NewHashMap);
        for (const auto& pair : map) {
            jbyteArray keyArray = env->NewByteArray((int) pair.first.size());
            jbyteArray valueArray = env->NewByteArray((int) pair.second.size());
            env->SetByteArrayRegion(keyArray, 0, (int) pair.first.size(), (jbyte*)pair.first.c_str());
            env->SetByteArrayRegion(valueArray, 0, (int) pair.second.size(), (jbyte*)pair.second.c_str());
            auto key = (jstring) env->NewObject(String, NewString, keyArray, charset);
            auto value = (jstring) env->NewObject(String, NewString, valueArray, charset);
            env->CallObjectMethod(hashMap, put, key, value);
        }
        env->CallBooleanMethod(arrayList, ArrayListAdd, hashMap);
    }

    return arrayList;
}