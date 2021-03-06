package org.ros.android.android_tutorial_teleop;

/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.Vertices;
import org.ros.android.view.visualization.layer.SubscriberLayer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;
import sensor_msgs.LaserScan;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * A {@link SubscriberLayer} that visualizes sensor_msgs/LaserScan messages.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class NIFTILaserScanPointLayer extends SubscriberLayer<sensor_msgs.LaserScan> implements TfLayer {

  private static final Color FREE_SPACE_COLOR = Color.fromHexAndAlpha("00adff", 0.3f);
  private static final Color OCCUPIED_SPACE_COLOR = Color.fromHexAndAlpha("FF0000", 0.6f);
  private static final float LASER_SCAN_POINT_SIZE = 0.1f; // M
  private static final int LASER_SCAN_STRIDE = 5;

  private final Object mutex;

  private GraphName frame;
  private FloatBuffer vertices;
  private Camera camera;

  public NIFTILaserScanPointLayer(String topicName) {
    this(GraphName.of(topicName));
  }

  public NIFTILaserScanPointLayer(GraphName topicName) {
    super(topicName, sensor_msgs.LaserScan._TYPE);
    mutex = new Object();
  }

  @Override
  public void draw(GL10 gl) {
    if (vertices != null) {
      synchronized (mutex) {
//        Vertices.drawTriangleFan(gl, vertices, FREE_SPACE_COLOR);
        Vertices.drawPoints(gl, vertices, OCCUPIED_SPACE_COLOR, 
        		(float)(LASER_SCAN_POINT_SIZE * camera.getZoom()));
      }
    }
  }

  @Override
  public void onStart(ConnectedNode connectedNode, android.os.Handler handler,
      FrameTransformTree frameTransformTree, Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    this.camera = camera;
    Subscriber<LaserScan> subscriber = getSubscriber();
    subscriber.addMessageListener(new MessageListener<LaserScan>() {
      @Override
      public void onNewMessage(LaserScan laserScan) {
        frame = GraphName.of(laserScan.getHeader().getFrameId());
        FloatBuffer vertices = newVertexBuffer(laserScan, LASER_SCAN_STRIDE);
        synchronized (mutex) {
        	NIFTILaserScanPointLayer.this.vertices = vertices;
        }
      }
    });
  }

  private FloatBuffer newVertexBuffer(LaserScan laserScan, int stride) {
    float[] ranges = laserScan.getRanges();
    int size = ((ranges.length / stride) + 2) * 3;
    FloatBuffer vertices = Vertices.allocateBuffer(size);
    // We start with the origin of the triangle fan.
    vertices.put(0);
    vertices.put(0);
    vertices.put(0);
    float minimumRange = laserScan.getRangeMin();
    float maximumRange = laserScan.getRangeMax();
    float angle = laserScan.getAngleMin();
    float angleIncrement = laserScan.getAngleIncrement();
    // Calculate the coordinates of the laser range values.
    for (int i = 0; i < ranges.length; i += stride) {
      float range = ranges[i];
      // Ignore ranges that are outside the defined range. We are not overly
      // concerned about the accuracy of the visualization and this is makes it
      // look a lot nicer.
      if (minimumRange < range && range < maximumRange) {
        // x, y, z
        vertices.put((float) (range * Math.cos(angle)));
        vertices.put((float) (range * Math.sin(angle)));
        vertices.put(0);
      }
      angle += angleIncrement * stride;
    }
    vertices.position(0);
    return vertices;
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}

