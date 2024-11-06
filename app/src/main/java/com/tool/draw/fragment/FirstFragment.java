package com.tool.draw.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tool.draw.R;
import com.tool.draw.databinding.FragmentFirstBinding;
import com.tool.draw.network.ConnectToServer;
import com.tool.draw.network.ConnectionViewModel;
import com.tool.draw.network.PendingMessage;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {
    private final Handler retryHandler = new Handler();
    private PendingMessage pendingMessage = null; // Store the pending message to be sent
    private ConnectToServer connectToServer;
    private String type = "";
    private ConnectionViewModel connectionViewModel;
    private Runnable retryRunnable;
    private boolean isResumedConnectionAttempted = false;
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        setupSpinner(binding.getRoot());
        connectionViewModel = new ViewModelProvider(requireActivity()).get(ConnectionViewModel.class);
        connectToServer = new ConnectToServer();
        connectToServer.setResponseTextView(binding.responseTextView);
        binding.swConnect.setChecked(true);
        handleToggleConnect(true);
        connectToServer.setConnectionViewModel(connectionViewModel);
        binding.swConnect.setOnCheckedChangeListener((buttonView, isChecked) -> handleToggleConnect(isChecked));
        binding.swOff.setOnCheckedChangeListener((buttonView, isChecked) -> handleToggleOnOff(isChecked));
        binding.sendButton.setOnClickListener(v -> handleSendButtonClick());

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSpinner(View view) {
        Spinner dropdown = view.findViewById(R.id.spinner); // Get the Spinner from the inflated view
        List<String> numbersList = new ArrayList<>();
        for (int i = 0; i <= 99; i++) {
            numbersList.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, numbersList);
        dropdown.setAdapter(adapter);

        ListPopupWindow listPopupWindow = new ListPopupWindow(requireContext());
        listPopupWindow.setAnchorView(dropdown);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setHeight(450);
        listPopupWindow.setModal(true);

        dropdown.setOnTouchListener((v, event) -> {
            listPopupWindow.show();
            return true;
        });

        listPopupWindow.setOnItemClickListener((parent, view1, position, id) -> {
            dropdown.setSelection(position);
            listPopupWindow.dismiss();
        });
    }

    private void handleToggleConnect(boolean isChecked) {
        if (isChecked) {
            connectToServer.connectToServer(getActivity());
            retryRunnable = new Runnable() {
                @Override
                public void run() {
                    Boolean isConnected = connectionViewModel.getConnectionStatus().getValue();
                    if (isConnected == null || !isConnected) {
                        Log.d("ConnectServer", "Retrying server connection...");
                        connectToServer.connectToServer(getActivity());
                        retryHandler.postDelayed(this, 5000); // Retry every 5 seconds
                    } else {
                        retryHandler.removeCallbacks(retryRunnable);
                        Log.d("ConnectServer", "Connection established, stopping retries.");
                    }
                }
            };
            retryHandler.postDelayed(retryRunnable, 5000); // Start retrying in 5 seconds
        } else {
            connectToServer.disconnect(); // Call the disconnect method
            connectionViewModel.setConnectionStatus(false); // Update connection status
            connectToServer.updateResponseText("切断");
            retryHandler.removeCallbacks(retryRunnable); // Stop retries if toggle is off
        }
    }

    private void handleToggleOnOff(boolean isChecked) {
        type = "turnonoff";
        int value = isChecked ? 1 : 0;
        if (binding.swConnect.isChecked()) {
            if (connectToServer.isConnected()) {
                connectToServer.sendMessageToServer(type, value);
            } else {
                connectToServer.setPendingMessage(type, value);
            }
        } else {
            connectToServer.setPendingMessage(type, value);
        }
    }

    private void handleSendButtonClick() {
        type = "sendnumber";
        int selectedNumber = Integer.parseInt(binding.spinner.getSelectedItem().toString());
        if (connectToServer.isConnected()) {
            connectToServer.sendMessageToServer(type, selectedNumber);
        } else {
            connectToServer.setPendingMessage(type, selectedNumber); // Store the pending message to be sent later
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding.swConnect.isChecked()) {
            Log.d("ConnectionStatus", "toggle_connect is ON. Attempting to connect to server.");
            if (!isResumedConnectionAttempted || !connectToServer.isConnected()) {
                isResumedConnectionAttempted = true;
                connectToServer.connectToServer(requireActivity());
                Log.d("ConnectionStatus", "Attempting to connect to server in onResume.");
                if (!connectToServer.isConnected()) {
                    Log.d("ConnectServer", "Failed to connect to server");
                }
                if (pendingMessage != null) {
                    connectToServer.sendMessageToServer(pendingMessage.getName(), pendingMessage.getCheckNumber());
                    pendingMessage = null; // Clear the pending message after sending
                }
            } else {
                Log.d("ConnectionStatus", "Already connected to server.");
            }
        } else {
            Log.d("ConnectionStatus", "toggle_connect is off. Not connected to server.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectionViewModel.setConnectionStatus(false); // Update connection status
    }
}
