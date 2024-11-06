package com.tool.draw.network;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tool.draw.activity.MainActivity;
import com.tool.draw.fragment.SecondFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class ConnectToServer {
    public static final String SEVER_IP = "192.168.179.17";
    public static final int SERVER_PORT = 8000;
    private static final String TAG = "ConnectServer";
    private final Handler handler = new Handler();
    public Socket client;
    private boolean isReconnecting = false;
    private PendingMessage pendingMessage = null;
    private TextView responseTextView;
    private OutputStream outputStream;
    private InputStream inputStream;
    private ConnectionViewModel connectionViewModel;

    public void setResponseTextView(TextView responseTextView) {
        this.responseTextView = responseTextView; // Assign TextView from MainActivity
    }

    public void setPendingMessage(String messageType, int messageValue) {
        this.pendingMessage = new PendingMessage(messageType, messageValue);
        Log.d(TAG, "Pending message stored: " + messageType + " - " + messageValue);

    }

    public void setConnectionViewModel(ConnectionViewModel viewModel) {
        this.connectionViewModel = viewModel;
        Log.d("ConnectToServer", "ConnectionViewModel set: " + viewModel);
    }

    public void connectToServer(Context context) {
        new Thread(() -> {
            try {
                if (client == null || client.isClosed()) {
                    client = new Socket(SEVER_IP, SERVER_PORT);
                    Log.d(TAG, "Connected to server");
                }

                if (client != null && client.isConnected()) {
                    Log.d("ConnectionStatus", "Connection successful");
                    if (connectionViewModel != null) {
                        connectionViewModel.setConnectionStatus(true); // Update connection status
                        ((MainActivity) context).runOnUiThread(() -> {
                            updateResponseText("接続");
                        });
                    } else {
                        Log.d(TAG, "ConnectionViewModel is null, unable to update connection status.");
                    }
                    if (pendingMessage != null) {
                        Log.d(TAG, "Pending message found. Sending message...");
                        sendMessageToServer(pendingMessage.getName(), pendingMessage.getCheckNumber());
                        pendingMessage = null;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        Log.d(TAG, "Response from server: " + serverResponse);
                        String finalResponse = serverResponse;
                        ((MainActivity) context).runOnUiThread(() -> {
                            if (responseTextView != null) {
                                updateResponseText(finalResponse);
                            }
                        });
                    }
                } else {
                    Log.d(TAG, "Not connected to server!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Error connecting to server", e);
            }
        }).start();
    }

    public void sendMessageToServer(String type, int value) {
        new Thread(() -> {
            try {
                if (isConnected()) {
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("type", type);
                    jsonMessage.put("value", value);

                    if (client != null && !client.isClosed()) {
                        outputStream = client.getOutputStream();
                    } else {
                        Log.e(TAG, "Connection lost before sending message.");
                        return;
                    }
                    if (outputStream != null) {
                        synchronized (this) {
                            outputStream.write((jsonMessage + "\n").getBytes());
                            outputStream.flush();
                        }
                        Log.d("ClientThread", "Message sent to server: " + jsonMessage);
                    } else {
                        Log.e("ClientThread", "OutputStream is null. Cannot send message.");
                    }
                } else {
                    Log.e("ClientThread", "Connection not established. Message not sent.");
                }
            } catch (IOException | JSONException e) {
                Log.d(TAG, "Error sending message to server", e);
            }
        }).start();
    }

    public boolean isConnected() {
        return client != null && client.isConnected() && !client.isClosed();
    }

    public void updateResponseText(String response) {
        if (responseTextView != null) {
            responseTextView.setText(response);
            responseTextView.setVisibility(View.VISIBLE);
            handler.postDelayed(() -> responseTextView.setVisibility(View.GONE), 3000); // Hide after 3 seconds
        }
    }

    public void connect() {
        new Thread(() -> {
            try {
                if (client == null || client.isClosed()) {
                    client = new Socket(SEVER_IP, SERVER_PORT);
                }
                client.setSoTimeout(30000);
                outputStream = client.getOutputStream();
                inputStream = client.getInputStream();
                Log.d(TAG, "Connected to server");
            } catch (IOException e) {
                Log.d(TAG, "Error connecting to server: " + e.getMessage(), e);
            }
        }).start();
    }

    private void ensureConnected() {
        if (!isConnected() && !isReconnecting) {
            isReconnecting = true;
            connect();
            isReconnecting = false;
        }
    }

    public void sendImage(byte[] imageData) {
        new Thread(() -> {
            ensureConnected();
            try {
                outputStream = client.getOutputStream();
                inputStream = client.getInputStream();
                if (isConnected()) {
                    if (imageData == null || imageData.length == 0) {
                        Log.d(TAG, "Image data is null or empty");
                        return;
                    }

                    JSONObject jsonMessage_img = new JSONObject();
                    jsonMessage_img.put("type", "sendimage");
                    jsonMessage_img.put("value", SecondFragment.currentNumber);
                    synchronized (this) {
                        outputStream.write((jsonMessage_img + "\n").getBytes());
                        outputStream.flush();
                    }
                    Thread.sleep(100);
                    int imageLength = imageData.length;
                    synchronized (this) {
                        outputStream.write((imageLength + "\n").getBytes());
                        outputStream.flush();
                    }

                    Thread.sleep(100);
                    synchronized (this) {
                        outputStream.write(imageData);
                        outputStream.flush();
                    }

                    Log.d(TAG, "Image sent to server");

                    byte[] responseBuffer = new byte[4096];
                    int bytesRead = inputStream.read(responseBuffer); // Read response
                    if (bytesRead > 0) {
                        String response = new String(responseBuffer, 0, bytesRead);
                        Log.d(TAG, "Server response: " + response);
                    } else {
                        Log.d(TAG, "No response from server");
                    }
                } else {
                    Log.d(TAG, "Socket is not connected");
                }
            } catch (SocketException e) {
                Log.d(TAG, "Socket error: " + e.getMessage(), e);
                reconnect();
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "Socket timeout: " + e.getMessage(), e);
                reconnect();
            } catch (IOException | JSONException e) {
                Log.d(TAG, "Error sending image: " + e.getMessage(), e);
                reconnect();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void reconnect() {
        disconnect();
        connect();
    }

    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (client != null && !client.isClosed()) {
                client.close();
                Log.d(TAG, "Socket closed");
            }
        } catch (IOException e) {
            Log.d(TAG, "Error closing socket", e);
        }
    }
}

