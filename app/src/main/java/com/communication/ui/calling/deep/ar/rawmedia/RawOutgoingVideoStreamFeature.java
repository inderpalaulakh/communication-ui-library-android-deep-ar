// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.communication.ui.calling.deep.ar.rawmedia;

import android.content.Context;

import com.azure.android.communication.calling.Call;
import com.azure.android.communication.calling.JoinCallOptions;
import com.azure.android.communication.calling.OutgoingVideoStream;
import com.azure.android.communication.calling.PixelFormat;
import com.azure.android.communication.calling.RawOutgoingVideoStream;
import com.azure.android.communication.calling.RawOutgoingVideoStreamOptions;
import com.azure.android.communication.calling.VideoFormat;
import com.azure.android.communication.calling.VideoFrameKind;
import com.azure.android.communication.calling.VideoOptions;
import com.azure.android.communication.calling.VirtualRawOutgoingVideoStream;

import java.util.Collections;

/**
 * Manages interactions between the outgoing video strema and the call
 *
 * @author yassirb@microsoft.com
 */
public class RawOutgoingVideoStreamFeature {

    protected FrameGenerator frameGenerator;
    protected RawOutgoingVideoStreamOptions options;
    protected RawOutgoingVideoStream stream;

    /**
     * Add the video options to the call
     *
     * @param joinCallOptions object that will contains the new video options
     * @return JoinCallOptions with the new video options
     */
    public JoinCallOptions addCallVideoOptions(JoinCallOptions joinCallOptions) {

        OutgoingVideoStream[] outgoingVideoStreamArray = {stream};
        VideoOptions videoOptions = new VideoOptions(outgoingVideoStreamArray);
        joinCallOptions.setVideoOptions(videoOptions);

        return joinCallOptions;
    }

    /**
     * Start the outgoing video stream from the call
     *
     * @param context
     * @param call
     */
    public void startVideo(Context context, Call call) {
        frameGenerator = new FrameGenerator(context);
        createOutgoingVideoStreamOptions(frameGenerator);
        stream = new VirtualRawOutgoingVideoStream(options);
        call.startVideo(context, stream);
    }

    /**
     * Stop the outgoing video stream from the call
     *
     * @param context
     * @param call
     */
    public void stopVideo(Context context, Call call) {
        call.stopVideo(context, stream);
    }

    /**
     * Initailzes video options using an outgoing video stream
     *
     * @param frameGenerator object that will generate frames
     */
    protected void createOutgoingVideoStreamOptions(FrameGenerator frameGenerator) {

        VideoFormat videoFormat = new VideoFormat();
        videoFormat.setWidth(1280);
        videoFormat.setHeight(720);
        videoFormat.setPixelFormat(PixelFormat.RGBA);
        videoFormat.setVideoFrameKind(VideoFrameKind.VIDEO_SOFTWARE);
        videoFormat.setFramesPerSecond(30);
        //videoFormat.setStride1(1280);
        videoFormat.setStride1((1280) * 4);

        options = new RawOutgoingVideoStreamOptions();
        options.setVideoFormats(Collections.singletonList(videoFormat));
        options.addOnVideoFrameSenderChangedListener(frameGenerator);
    }
}
