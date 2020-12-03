/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.paddlepaddle.jna;

import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.paddlepaddle.engine.PpDataType;
import ai.djl.paddlepaddle.engine.PpNDArray;
import ai.djl.paddlepaddle.engine.PpNDManager;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * A class containing utilities to interact with the PaddlePaddle Engine's Java Native Access (JNA)
 * layer.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class JnaUtils {

    private static final PaddleLibrary LIB = LibUtils.loadLibrary();

    private JnaUtils() {}

    public static PpNDArray createNdArray(
            PpNDManager manager, Buffer data, Shape shape, DataType dtype) {
        Pointer tensor = LIB.PD_NewPaddleTensor();
        LIB.PD_SetPaddleTensorDType(tensor, PpDataType.toPaddlePaddle(dtype));
        long[] shapes = shape.getShape();
        int[] size = Arrays.stream(shapes).mapToInt(Math::toIntExact).toArray();
        LIB.PD_SetPaddleTensorShape(tensor, size, size.length);

        Pointer paddleBuffer = LIB.PD_NewPaddleBuf();
        long length = dtype.getNumOfBytes() * shape.size();
        Pointer pointer = Native.getDirectBufferPointer(data);
        LIB.PD_PaddleBufReset(paddleBuffer, pointer, length);
        LIB.PD_SetPaddleTensorData(tensor, paddleBuffer);
        return new PpNDArray(manager, tensor, shape, dtype);
    }

    public static Pointer getBufferPointerFromNd(PpNDArray array) {
        Pointer bufHandle = LIB.PD_GetPaddleTensorData(array.getHandle());
        return LIB.PD_PaddleBufData(bufHandle);
    }

    public static ByteBuffer getByteBufferFromNd(PpNDArray array) {
        Pointer bufHandle = LIB.PD_GetPaddleTensorData(array.getHandle());
        int length = Math.toIntExact(LIB.PD_PaddleBufLength(bufHandle));
        Pointer buf = LIB.PD_PaddleBufData(bufHandle);
        byte[] bytes = new byte[length];
        buf.read(0, bytes, 0, length);
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
    }

    public static void freeNdArray(Pointer tensor) {
        LIB.PD_DeletePaddleTensor(tensor);
    }

    public static void setNdArrayName(Pointer tensor, String name) {
        LIB.PD_SetPaddleTensorName(tensor, name);
    }

    public static String getNdArrayName(Pointer tensor) {
        return LIB.PD_GetPaddleTensorName(tensor);
    }

    public static DataType getDataType(Pointer pointer) {
        int type = LIB.PD_GetPaddleTensorDType(pointer);
        return PpDataType.fromPaddlePaddle(type);
    }

    public static String getVersion() {
        return "2.0.0";
    }

    public static AnalysisConfig newAnalysisConfig() {
        return new AnalysisConfig(LIB.PD_NewAnalysisConfig());
    }

    public static void loadModel(AnalysisConfig config, String modelDir, String paramsPath) {
        if (paramsPath == null) {
            paramsPath = modelDir;
        }
        LIB.PD_SetModel(config.getHandle(), modelDir, paramsPath);
    }
}
