package io.agora.metachat.example.metachat;


import android.content.Context;
import android.util.Log;
import android.view.TextureView;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.base.VideoFrame;
import io.agora.metachat.example.models.RoleInfo;
import io.agora.metachat.example.models.UnityMessage;
import io.agora.metachat.example.models.UnityRoleInfo;
import io.agora.metachat.example.utils.KeyCenter;
import io.agora.metachat.example.utils.MMKVUtils;
import io.agora.metachat.AvatarModelInfo;
import io.agora.metachat.DressInfo;
import io.agora.metachat.EnterSceneConfig;
import io.agora.metachat.ILocalUserAvatar;
import io.agora.metachat.IMetachatEventHandler;
import io.agora.metachat.IMetachatScene;
import io.agora.metachat.IMetachatSceneEventHandler;
import io.agora.metachat.IMetachatService;
import io.agora.metachat.MetachatConfig;
import io.agora.metachat.MetachatSceneConfig;
import io.agora.metachat.MetachatSceneInfo;
import io.agora.metachat.MetachatUserInfo;
import io.agora.metachat.MetachatUserPositionInfo;
import io.agora.metachat.example.inf.IRtcEventCallback;
import io.agora.metachat.example.models.EnterSceneExtraInfo;
import io.agora.metachat.example.utils.MetaChatConstants;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.spatialaudio.ILocalSpatialAudioEngine;
import io.agora.spatialaudio.LocalSpatialAudioConfig;
import io.agora.spatialaudio.RemoteVoicePositionInfo;

public class MetaChatContext implements IMetachatEventHandler, IMetachatSceneEventHandler {

    private final static String TAG = MetaChatContext.class.getSimpleName();
    private volatile static MetaChatContext instance = null;
    private final static boolean enableSpatialAudio = true;

    private RtcEngine rtcEngine;
    private ILocalSpatialAudioEngine spatialAudioEngine;
    private IMetachatService metaChatService;
    private IMetachatScene metaChatScene;
    private MetachatSceneInfo sceneInfo;
    private AvatarModelInfo modelInfo;
    private MetachatUserInfo userInfo;
    private String roomName;
    private TextureView sceneView;
    private final ConcurrentHashMap<IMetachatEventHandler, Integer> metaChatEventHandlerMap;
    private final ConcurrentHashMap<IMetachatSceneEventHandler, Integer> metaChatSceneEventHandlerMap;
    private boolean mJoinedRtc = false;
    private ILocalUserAvatar localUserAvatar;
    private boolean isInScene;
    private int currentScene;
    private int nextScene;
    private RoleInfo roleInfo;
    private List<RoleInfo> roleInfos;
    private boolean needSaveDressInfo;
    private boolean isInitMetachat;
    private String broadcasterUserId;

    private IRtcEventCallback iRtcEventCallback;

    private int clientRoleType;


    private MetaChatContext() {
        metaChatEventHandlerMap = new ConcurrentHashMap<>();
        metaChatSceneEventHandlerMap = new ConcurrentHashMap<>();
        isInScene = false;
        currentScene = MetaChatConstants.SCENE_NONE;
        nextScene = MetaChatConstants.SCENE_NONE;
        roleInfo = null;
        needSaveDressInfo = false;
        isInitMetachat = false;
        iRtcEventCallback = null;
        clientRoleType = -1;
    }

    public static MetaChatContext getInstance() {
        if (instance == null) {
            synchronized (MetaChatContext.class) {
                if (instance == null) {
                    instance = new MetaChatContext();
                }
            }
        }
        return instance;
    }

    public boolean initialize(Context context) {
        int ret = Constants.ERR_OK;
        if (rtcEngine == null) {
            try {
                rtcEngine = RtcEngine.create(context, KeyCenter.APP_ID, new IRtcEngineEventHandler() {
                    @Override
                    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                        Log.d(TAG, String.format("onJoinChannelSuccess %s %d", channel, uid));
                        mJoinedRtc = true;
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onJoinChannelSuccess(channel, uid, elapsed);
                        }
                    }

                    @Override
                    public void onLeaveChannel(RtcStats stats) {
                        Log.d(TAG, "onLeaveChannel stats=" + stats);
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onLeaveChannel(stats);
                        }
                    }

                    @Override
                    public void onUserOffline(int uid, int reason) {
                        Log.d(TAG, String.format("onUserOffline %d %d ", uid, reason));
                        if (spatialAudioEngine != null) {
                            spatialAudioEngine.removeRemotePosition(uid);
                        }
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onUserOffline(uid, reason);
                        }
                    }

                    @Override
                    public void onAudioRouteChanged(int routing) {
                        Log.d(TAG, String.format("onAudioRouteChanged %d", routing));
                    }

                    @Override
                    public void onUserJoined(int uid, int elapsed) {
                        Log.d(TAG, "onUserJoined uid=" + uid);
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onUserJoined(uid, elapsed);
                        }
                    }

                    @Override
                    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
                        Log.d(TAG, "onFirstRemoteVideoDecoded uid=" + uid + ",width=" + width + ",heigh=" + height + ",elapsed=" + elapsed);
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
                        }
                    }
                });
               /* rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                        new VideoEncoderConfiguration.VideoDimensions(640, 480),
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                        STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT, VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED));*/

                //rtcEngine.setParameters("{\"rtc.enable_debug_log\":true}");
                rtcEngine.enableAudio();
                rtcEngine.enableVideo();
                rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
                rtcEngine.setAudioProfile(
                        Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_GAME_STREAMING
                );
                rtcEngine.setExternalVideoSource(true, true, Constants.ExternalVideoSourceType.VIDEO_FRAME);


                {
                    metaChatService = IMetachatService.create();
                    MetachatConfig config = new MetachatConfig() {{
                        mRtcEngine = rtcEngine;
                        mAppId = KeyCenter.APP_ID;
                        mRtmToken = KeyCenter.RTM_TOKEN;
                        mLocalDownloadPath = context.getExternalCacheDir().getPath();
                        mUserId = KeyCenter.RTM_UID;
                        mEventHandler = MetaChatContext.this;
                    }};
                    ret += metaChatService.initialize(config);
                    Log.i(TAG, "launcher version=" + metaChatService.getLauncherVersion(context));
                }

                isInitMetachat = true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return ret == Constants.ERR_OK;
    }

    public void destroy() {
        if (spatialAudioEngine != null) {
            ILocalSpatialAudioEngine.destroy();
            spatialAudioEngine = null;
        }
        if (isInitMetachat) {
            IMetachatService.destroy();
            metaChatService = null;
            isInitMetachat = false;
        }

        RtcEngine.destroy();
        rtcEngine = null;

        roleInfo = null;
        clientRoleType = -1;
    }

    public void registerMetaChatEventHandler(IMetachatEventHandler eventHandler) {
        metaChatEventHandlerMap.put(eventHandler, 0);
    }

    public void unregisterMetaChatEventHandler(IMetachatEventHandler eventHandler) {
        metaChatEventHandlerMap.remove(eventHandler);
    }

    public void registerMetaChatSceneEventHandler(IMetachatSceneEventHandler eventHandler) {
        metaChatSceneEventHandlerMap.put(eventHandler, 0);
    }

    public void unregisterMetaChatSceneEventHandler(IMetachatSceneEventHandler eventHandler) {
        metaChatSceneEventHandlerMap.remove(eventHandler);
    }

    public boolean getSceneInfos() {
        return metaChatService.getSceneInfos() == Constants.ERR_OK;
    }

    public boolean isSceneDownloaded(MetachatSceneInfo sceneInfo) {
        return metaChatService.isSceneDownloaded(sceneInfo.mSceneId) > 0;
    }

    public boolean downloadScene(MetachatSceneInfo sceneInfo) {
        return metaChatService.downloadScene(sceneInfo.mSceneId) == Constants.ERR_OK;
    }

    public boolean cancelDownloadScene(MetachatSceneInfo sceneInfo) {
        return metaChatService.cancelDownloadScene(sceneInfo.mSceneId) == Constants.ERR_OK;
    }

    public void prepareScene(MetachatSceneInfo sceneInfo, AvatarModelInfo modelInfo, MetachatUserInfo userInfo) {
        this.sceneInfo = sceneInfo;
        this.modelInfo = modelInfo;
        this.userInfo = userInfo;
    }

    public boolean createScene(Context activityContext, String roomName, TextureView tv) {
        Log.d(TAG, String.format("createAndEnterScene %s", roomName));
        this.roomName = roomName;
        this.sceneView = tv;

        if (isBroadcaster()) {
            if (spatialAudioEngine == null && enableSpatialAudio) {
                spatialAudioEngine = ILocalSpatialAudioEngine.create();
                LocalSpatialAudioConfig config = new LocalSpatialAudioConfig() {{
                    mRtcEngine = rtcEngine;
                }};
                spatialAudioEngine.initialize(config);
                spatialAudioEngine.muteLocalAudioStream(false);
                spatialAudioEngine.muteAllRemoteAudioStreams(false);
            }
        }

        MetachatSceneConfig sceneConfig = new MetachatSceneConfig();
        sceneConfig.mActivityContext = activityContext;
        if (isBroadcaster()) {
            sceneConfig.mSceneBroadcastMode = MetachatSceneConfig.SceneBroadcastMode.SCENE_BROADCAST_MODE_NONE;
        } else {
            sceneConfig.mSceneBroadcastMode = MetachatSceneConfig.SceneBroadcastMode.SCENE_BROADCAST_MODE_AUDIENCE;
        }
        int ret = -1;
        if (metaChatScene == null) {
            ret = metaChatService.createScene(sceneConfig);
        }
        if (isBroadcaster()) {
            mJoinedRtc = false;
        }
        return ret == Constants.ERR_OK;
    }

    public void enterScene() {
        Log.d(TAG, "enterScene");
        if (null != localUserAvatar) {
            localUserAvatar.setUserInfo(userInfo);
            if (isBroadcaster()) {
                //该model的mBundleType为MetachatBundleInfo.BundleType.BUNDLE_TYPE_AVATAR类型
                localUserAvatar.setModelInfo(modelInfo);
                if (null != roleInfo) {
                    //设置服装信息
                    DressInfo dressInfo = new DressInfo();
                    dressInfo.mExtraCustomInfo = (JSONObject.toJSONString(getUnityRoleInfo())).getBytes();
                    localUserAvatar.setDressInfo(dressInfo);
                }
            }
        }
        if (null != metaChatScene) {
            if (isBroadcaster()) {
                //使能位置信息回调功能
                metaChatScene.enableUserPositionNotification(MetaChatConstants.SCENE_GAME == MetaChatContext.getInstance().getCurrentScene());
            }
            //设置回调接口
            metaChatScene.addEventHandler(MetaChatContext.getInstance());
            EnterSceneConfig config = new EnterSceneConfig();
            //sceneView必须为Texture类型，为渲染unity显示的view
            config.mSceneView = this.sceneView;
            //rtc加入channel的ID
            config.mRoomName = this.roomName;
            if (null != sceneInfo) {
                //内容中心对应的ID
                config.mSceneId = this.sceneInfo.mSceneId;
            }

            /*
             *仅为示例格式，具体格式以项目实际为准
             *   "extraCustomInfo":{
             *     "sceneIndex":0  //0为默认场景，在这里指咖啡厅，1为换装设置场景
             *   }
             */
            EnterSceneExtraInfo extraInfo = new EnterSceneExtraInfo();
            if (MetaChatConstants.SCENE_DRESS == MetaChatContext.getInstance().getCurrentScene()) {
                extraInfo.setSceneIndex(MetaChatConstants.SCENE_DRESS);
            } else if (MetaChatConstants.SCENE_GAME == MetaChatContext.getInstance().getCurrentScene()) {
                extraInfo.setSceneIndex(MetaChatConstants.SCENE_GAME);
            } else if (MetaChatConstants.SCENE_IDOL == MetaChatContext.getInstance().getCurrentScene()) {
                extraInfo.setSceneIndex(MetaChatConstants.SCENE_IDOL);
            }
            //加载的场景index
            config.mExtraCustomInfo = JSONObject.toJSONString(extraInfo).getBytes();
            if (MetaChatConstants.SCENE_IDOL == MetaChatContext.getInstance().getCurrentScene() && !isBroadcaster()) {
                config.broadcaster = this.broadcasterUserId;
            }
            metaChatScene.enterScene(config);
        }

    }

    @Override
    public void onCreateSceneResult(IMetachatScene scene, int errorCode) {
        Log.d(TAG, "onCreateSceneResult errorCode:" + errorCode);
        metaChatScene = scene;
        localUserAvatar = metaChatScene.getLocalUserAvatar();
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onCreateSceneResult(scene, errorCode);
        }
    }


    public boolean updateRole(int role) {
        int ret = Constants.ERR_OK;
        //是否为broadcaster
        boolean isBroadcaster = role == Constants.CLIENT_ROLE_BROADCASTER;
        ret += rtcEngine.updateChannelMediaOptions(new ChannelMediaOptions() {{
            publishMicrophoneTrack = isBroadcaster;
            clientRoleType = role;
        }});
        modelInfo.mLocalVisible = true;

        if (MetaChatConstants.SCENE_GAME == MetaChatContext.getInstance().getCurrentScene()) {
            modelInfo.mRemoteVisible = isBroadcaster;
            modelInfo.mSyncPosition = isBroadcaster;
        } else if (MetaChatConstants.SCENE_DRESS == MetaChatContext.getInstance().getCurrentScene()) {
            modelInfo.mRemoteVisible = false;
            modelInfo.mSyncPosition = false;
        }

        if (null != localUserAvatar && Constants.ERR_OK == localUserAvatar.setModelInfo(modelInfo)) {
            ret += localUserAvatar.applyInfo();
        }
        return ret == Constants.ERR_OK;
    }

    public void updatePublishMediaOptions(boolean enable, int playerId) {
        if (null != rtcEngine) {
            rtcEngine.updateChannelMediaOptions(new ChannelMediaOptions() {{
                publishMediaPlayerAudioTrack = enable;
                publishMediaPlayerId = playerId;

            }});
        }
    }

    public boolean enableLocalAudio(boolean enabled) {
        return rtcEngine.enableLocalAudio(enabled) == Constants.ERR_OK;
    }

    public boolean muteAllRemoteAudioStreams(boolean mute) {
        if (spatialAudioEngine != null) {
            return spatialAudioEngine.muteAllRemoteAudioStreams(mute) == Constants.ERR_OK;
        }
        return rtcEngine.muteAllRemoteAudioStreams(mute) == Constants.ERR_OK;
    }

    public boolean leaveScene() {
        Log.d(TAG, "leaveScene");
        int ret = Constants.ERR_OK;
        if (metaChatScene != null) {
            ret += leaveRtcChannel();
            mJoinedRtc = false;
            ret += metaChatScene.leaveScene();
        }
        Log.d(TAG, "leaveScene success");
        return ret == Constants.ERR_OK;
    }

    public int leaveRtcChannel() {
        if (null == rtcEngine) {
            return Constants.ERR_FAILED;
        }
        return rtcEngine.leaveChannel();
    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {
        Log.d(TAG, "onConnectionStateChanged state=" + state + ",reason=" + reason);
        if (state == ConnectionState.METACHAT_CONNECTION_STATE_ABORTED) {
            setCurrentScene(MetaChatConstants.SCENE_NONE);
            resetRoleInfo();
            leaveScene();
        }

        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onConnectionStateChanged(state, reason);
        }
    }

    @Override
    public void onRequestToken() {
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onRequestToken();
        }
    }

    @Override
    public void onGetSceneInfosResult(MetachatSceneInfo[] scenes, int errorCode) {
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onGetSceneInfosResult(scenes, errorCode);
        }
    }

    @Override
    public void onDownloadSceneProgress(long SceneId, int progress, int state) {
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onDownloadSceneProgress(SceneId, progress, state);
        }
    }

    @Override
    public void onSceneVideoFrame(VideoFrame videoFrame) {
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onSceneVideoFrame(videoFrame);
        }
    }

    @Override
    public void onRecvMessageFromUser(String userId, byte[] message) {
        Log.d(TAG, "onRecvMessageFromUser");
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onRecvMessageFromUser(userId, message);
        }
    }

    @Override
    public void onEnterSceneResult(int errorCode) {
        Log.d(TAG, String.format("onEnterSceneResult %d", errorCode));
        if (errorCode == 0) {
            isInScene = true;

            if (needSaveDressInfo) {
                needSaveDressInfo = false;
                MMKVUtils.getInstance().putValue(MetaChatConstants.MMKV_ROLE_INFO, JSONArray.toJSONString(roleInfos));
            }

            if (null != metaChatScene) {
                metaChatScene.setSceneParameters("{\"debugUnity\":true}");
                metaChatScene.enableSceneBroadcast(true);
                if (isBroadcaster()) {
                    metaChatScene.enableSceneVideo(true);
                }
            }
            if (isBroadcaster()) {
                joinChannel(Constants.CLIENT_ROLE_BROADCASTER);
                if (MetaChatConstants.SCENE_GAME == MetaChatContext.getInstance().getCurrentScene()) {
                    pushVideoFrameToDisplay();
                }
                if (spatialAudioEngine != null) {
                    // audio的mute状态交给ILocalSpatialAudioEngine统一管理
                    rtcEngine.muteAllRemoteAudioStreams(true);
                }
            }
        }
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onEnterSceneResult(errorCode);
        }
    }

    public void joinChannel(int roleType) {
        int ret = rtcEngine.joinChannel(
                KeyCenter.RTC_TOKEN, KeyCenter.CHANNEL_ID, KeyCenter.RTC_UID,
                new ChannelMediaOptions() {{
                    publishMicrophoneTrack = true;
                    autoSubscribeAudio = true;
                    clientRoleType = roleType;
                }});
        Log.d(TAG, String.format("joinChannel ret %d", ret));
    }

    private void pushVideoFrameToDisplay() {
        metaChatScene.enableVideoDisplay("1", true);
    }

    @Override
    public void onLeaveSceneResult(int errorCode) {
        Log.d(TAG, String.format("onLeaveSceneResult %d", errorCode));
        isInScene = false;
        if (errorCode == 0) {
            metaChatScene.release();
            metaChatScene = null;
        }

        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onLeaveSceneResult(errorCode);
        }
    }

    @Override
    public void onReleasedScene(int status) {
        Log.d(TAG, String.format("onReleasedScene %d", status));
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onReleasedScene(status);
        }
    }

    @Override
    public void onRecvMessageFromScene(byte[] message) {
        Log.d(TAG, String.format("onRecvMessageFromScene %s", new String(message)));
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onRecvMessageFromScene(message);
        }
    }

    @Override
    public void onUserPositionChanged(String uid, MetachatUserPositionInfo posInfo) {
        Log.d(TAG, String.format("onUserPositionChanged %s %s %s %s %s", uid,
                Arrays.toString(posInfo.mPosition),
                Arrays.toString(posInfo.mForward),
                Arrays.toString(posInfo.mRight),
                Arrays.toString(posInfo.mUp)
        ));

        if (spatialAudioEngine != null) {
            try {
                int userId = Integer.parseInt(uid);
                if (KeyCenter.RTC_UID == userId) {
                    spatialAudioEngine.updateSelfPosition(
                            posInfo.mPosition, posInfo.mForward, posInfo.mRight, posInfo.mUp
                    );
                } else if (mJoinedRtc) {
                    spatialAudioEngine.updateRemotePosition(userId, new RemoteVoicePositionInfo() {{
                        position = posInfo.mPosition;
                        forward = posInfo.mForward;
                    }});
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onUserPositionChanged(uid, posInfo);
        }
    }

    @Override
    public void onEnumerateVideoDisplaysResult(String[] displayIds) {

    }

    public MetachatSceneInfo getSceneInfo() {
        return sceneInfo;
    }

    public void pushVideoFrameToDisplay(VideoFrame frame) {
        if (null != metaChatScene) {
            metaChatScene.pushVideoFrameToDisplay("1", frame);
        }
    }

    public boolean isInScene() {
        return isInScene;
    }

    public void initRoleInfo(String name, int gender) {
        roleInfo = null;
        initRoleInfoFromDb(name, gender);

        if (null == roleInfo) {
            if (MetaChatConstants.SCENE_IDOL != currentScene) {
                currentScene = MetaChatConstants.SCENE_DRESS;
            }

            if (null == roleInfos) {
                roleInfos = new ArrayList<>(1);
            }

            roleInfo = new RoleInfo();
            roleInfo.setName(name);
            roleInfo.setGender(gender);
            //dress default id is 1
            roleInfo.setHair(1);
            roleInfo.setTops(1);
            roleInfo.setShoes(1);
            roleInfo.setLower(1);
            roleInfos.add(roleInfo);

            needSaveDressInfo = true;
        } else {
            if (MetaChatConstants.SCENE_IDOL != currentScene) {
                currentScene = MetaChatConstants.SCENE_GAME;
            }
            needSaveDressInfo = false;
        }
    }


    public RoleInfo getRoleInfo() {
        return roleInfo;
    }

    public int getCurrentScene() {
        return currentScene;
    }

    public int getNextScene() {
        return nextScene;
    }

    public void setCurrentScene(int currentScene) {
        this.currentScene = currentScene;
    }

    public void setNextScene(int nextScene) {
        this.nextScene = nextScene;
    }

    public void sendRoleDressInfo() {
        //注意该协议格式需要和unity协商一致
        UnityMessage message = new UnityMessage();
        message.setKey(MetaChatConstants.KEY_UNITY_MESSAGE_DRESS_SETTING);
        message.setValue(JSONObject.toJSONString(getUnityRoleInfo()));
        sendSceneMessage(JSONObject.toJSONString(message));
    }

    public void saveRoleDressInfo(String name, int gender) {
        Iterator<RoleInfo> it = roleInfos.iterator();
        RoleInfo tempRoleInfo;
        while (it.hasNext()) {
            tempRoleInfo = it.next();
            //just only gender
            if (/*tempRoleInfo.getName().equalsIgnoreCase(name) && */tempRoleInfo.getGender() == gender) {
                it.remove();
            }
        }
        roleInfos.add(0, roleInfo);
        MMKVUtils.getInstance().putValue(MetaChatConstants.MMKV_ROLE_INFO, JSONArray.toJSONString(roleInfos));
    }

    public void cancelRoleDressInfo(String name, int gender) {
        initRoleInfoFromDb(name, gender);
    }

    private void initRoleInfoFromDb(String name, int gender) {
        roleInfos = JSONArray.parseArray(MMKVUtils.getInstance().getValue(MetaChatConstants.MMKV_ROLE_INFO, ""), RoleInfo.class);

        if (roleInfos != null && roleInfos.size() != 0) {
            for (int i = 0; i < roleInfos.size(); i++) {
                if (null != roleInfos.get(i)) {
                    //only just gender
                    if (/*(null != roleInfos.get(i).getName() && roleInfos.get(i).getName().equalsIgnoreCase(name)) &&*/ roleInfos.get(i).getGender() == gender) {
                        roleInfo = roleInfos.get(i);
                        break;
                    }
                }
            }
        }

        if (null != roleInfo) {
            roleInfo.setName(name);
        }
    }

    public void sendSceneMessage(String msg) {
        if (metaChatScene == null) {
            Log.e(TAG, "sendMessageToScene metaChatScene is null");
            return;
        }

        if (metaChatScene.sendMessageToScene(msg.getBytes()) == 0) {
            Log.i(TAG, "sendMessageToScene " + msg + " successful");
        } else {
            Log.e(TAG, "sendMessageToScene " + msg + " fail");
        }
    }

    public void sendMessageToUser(String uid, String msg) {
        if (metaChatScene == null) {
            Log.e(TAG, "sendMessageToScene metaChatScene is null");
            return;
        }

        if (metaChatScene.sendMessageToUser(uid, msg.getBytes()) == 0) {
            Log.i(TAG, "sendMessageToUser:" + uid + " " + msg + " successful");
        } else {
            Log.e(TAG, "sendMessageToUser:" + uid + " " + msg + " fail");
        }
    }

    public UnityRoleInfo getUnityRoleInfo() {
        UnityRoleInfo unityRoleInfo = new UnityRoleInfo();
        unityRoleInfo.setGender(roleInfo.getGender());
        unityRoleInfo.setHair(roleInfo.getHair());
        unityRoleInfo.setTops(roleInfo.getTops());
        unityRoleInfo.setLower(roleInfo.getLower());
        unityRoleInfo.setShoes(roleInfo.getShoes());
        return unityRoleInfo;
    }

    public void resetRoleInfo() {
        roleInfos = null;
        roleInfo = null;
    }

    public boolean isInitMetachat() {
        return isInitMetachat;
    }

    public boolean pushExternalVideoFrame(VideoFrame videoFrame) {
        if (null == rtcEngine) {
            return false;
        }

        return rtcEngine.pushExternalVideoFrame(videoFrame);
    }

    public boolean isBroadcaster() {
        return Constants.CLIENT_ROLE_BROADCASTER == clientRoleType;
    }

    public void setRtcEventCallback(IRtcEventCallback iRtcEventCallback) {
        this.iRtcEventCallback = iRtcEventCallback;
    }

    public RtcEngine getRtcEngine() {
        return rtcEngine;
    }

    public void setBroadcasterUserId(String broadcasterUserId) {
        this.broadcasterUserId = broadcasterUserId;
    }

    public String getBroadcasterUserId() {
        return broadcasterUserId;
    }

    public void setClientRoleType(int clientRoleType) {
        this.clientRoleType = clientRoleType;
    }

    public int getClientRoleType() {
        return clientRoleType;
    }

    public int getSceneId() {
        if (MetaChatConstants.SCENE_IDOL == currentScene) {
            return MetaChatConstants.SCENE_ID_IDOL;
        } else {
            return MetaChatConstants.SCENE_ID_SDK_2_TEST;
        }
    }
}
