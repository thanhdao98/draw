package com.tool.draw.fragment;

import static android.app.Activity.RESULT_OK;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tool.draw.R;
import com.tool.draw.databinding.FragmentSecondBinding;
import com.tool.draw.dialog.ConfirmationDialog;
import com.tool.draw.network.ConnectToServer;
import com.tool.draw.network.ConnectionViewModel;
import com.tool.draw.views.CustomViewPager;
import com.tool.draw.views.ISeekBarListener;
import com.tool.draw.views.adapter.CustomSpinnerAdapter;

import java.io.ByteArrayOutputStream;

public class SecondFragment extends Fragment {
    public static final String[] NUMBER = new String[]{"90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};
    public static final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.BLACK, Color.WHITE};
    public static final String[] colorNames = {"赤", "緑", "青", "黄色", "シアン", "マゼンタ", "黒", "白"};
    public static final int TOOL_NEUTRAL = 0;
    public static final int TOOL_ERASER = 1;
    public static final int TOOL_BLACK_PEN = 2;
    public static int currentNumber;
    private int toolMode = TOOL_NEUTRAL;   // 描画ツール非選択状態
    private CustomViewPager viewPager;
    private ConnectToServer connectToServer;
    private boolean isMenuVisible = true; // Flag to check if menus are visible
    private int currentColor = Color.BLACK;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private boolean isConnected; // Variable to store connection status
    private FragmentSecondBinding bindView;
    private float startX, startY;
    private boolean isColorPickerSelected = false;
    private boolean isPenThicknessSelected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolMode = TOOL_NEUTRAL;
        ConnectionViewModel connectionViewModel = new ViewModelProvider(requireActivity()).get(ConnectionViewModel.class);
        connectionViewModel.getConnectionStatus().observe(this, connected -> {
            isConnected = connected; // Save connection status to a variable
        });

        connectToServer = new ConnectToServer();
        initializeImagePicker();
    }

    private void showThicknessAdjustment(int stateMenu, int stateSeekBar) {
        bindView.trDetailMenu.setVisibility(stateMenu);
        bindView.seekBarThickness.setVisibility(stateSeekBar);
    }

    private void showColorPicker() {
        Animation animation;
        if (!isColorPickerSelected) {
            animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_button_scale);
            isColorPickerSelected = true;
        } else {
            animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_button_scale_reverse);
            isColorPickerSelected = false;
        }
        bindView.ivSelectColor.startAnimation(animation);
        int selectedColorIndex = -1;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == currentColor) {
                selectedColorIndex = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("色を選択");
        builder.setSingleChoiceItems(colorNames, selectedColorIndex, (dialog, which) -> {
            currentColor = colors[which];
            bindView.drawingView.setPaintColor(currentColor);
            dialog.dismiss();
        });
        builder.show();
    }

    /**
     * Inflates the fragment layout and initializes the views.
     * Restores the tool mode if there is a saved instance state.
     */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bindView = FragmentSecondBinding.inflate(inflater, container, false);
        if (savedInstanceState != null) {
            toolMode = TOOL_NEUTRAL;
        }
        initViews();
        return bindView.getRoot();
    }

    /**
     * Initializes UI components and sets up click listeners for various actions.
     * This method configures the view pager, button actions, and tool modes,
     * enabling interactions for selecting images, colors, and drawing tools.
     */

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        viewPager = requireActivity().findViewById(R.id.viewPager);
        setupNumberSpinner();
        bindView.btSelectImage.setOnClickListener(v -> {
            hideSeekBar();
            openImageChooser();
        });
        bindView.ivSelectColor.setOnClickListener(v -> {
            hideSeekBar();
            showColorPicker();
        });
        bindView.drawingView.setToolMode(toolMode);
        bindView.ivPenThickness.setOnClickListener(v -> {
            Animation animation;
            if (!isPenThicknessSelected) {
                animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_button_scale);
                isPenThicknessSelected = true;
            } else {
                animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_button_scale_reverse);
                isPenThicknessSelected = false;
            }
            v.startAnimation(animation);
            int stateMenu = bindView.trDetailMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            int stateSeekBar = bindView.seekBarThickness.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            showThicknessAdjustment(stateMenu, stateSeekBar);
        });
        bindView.ivPencil.setOnClickListener(v -> {
            hideSeekBar();
            selectPen();
        });
        bindView.ivEraser.setOnClickListener(v -> {
            hideSeekBar();
            selectEraser();
        });
        bindView.ivUndo.setOnClickListener(v -> {
            bindView.drawingView.undo();
            hideSeekBar();
        });
        bindView.ivRedo.setOnClickListener(v -> {
            bindView.drawingView.redo();
            hideSeekBar();
        });
        bindView.drawingView.setOnTouchListener((v, event) -> handleOnTouch(event));
        bindView.btComplete.setOnClickListener(v -> showConfirmationDialog());
        bindView.drawingView.setToolMode(toolMode);
        bindView.seekBarThickness.setOnSeekBarChangeListener(new ISeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                bindView.drawingView.setBrushThickness(progress);
            }
        });
        updateToolSelectionUI();
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    bindView.drawingView.clear();
                    bindView.drawingView.loadImage(imageUri);
                    bindView.drawingView.resetUndoRedoStacks();
                }
            }
        });
    }

    private void hideSeekBar() {
        showThicknessAdjustment(View.GONE, View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupNumberSpinner() {
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(getContext(), NUMBER);
        bindView.numberSpinner.setAdapter(adapter);

        bindView.numberSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectedPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bindView.numberSpinner.setOnTouchListener((v, event) -> {
            hideSeekBar();
            return false;
        });
    }


    /**
     * Opens an image chooser to select an image from the device's gallery.
     * Sets the tool mode to neutral and updates the tool selection UI.
     */

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
        toolMode = TOOL_NEUTRAL;
        bindView.drawingView.setToolMode(toolMode);
        updateToolSelectionUI();
    }

    /**
     * Handles touch events for toggling menus when the tool mode is neutral.
     * Detects taps within a specified threshold and hides the seek bar.
     *
     * @param event The MotionEvent containing touch event details.
     * @return true if the event was handled, false otherwise.
     */

    private boolean handleOnTouch(MotionEvent event) {
        hideSeekBar();
        int TAP_THRESHOLD = 150;
        if (toolMode != TOOL_NEUTRAL) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float endX = event.getX();
                float endY = event.getY();
                float distanceX = Math.abs(endX - startX);
                float distanceY = Math.abs(endY - startY);
                if (distanceX < TAP_THRESHOLD && distanceY < TAP_THRESHOLD) {
                    toggleMenus();
                }
                break;
        }
        return true;
    }

    private void showConfirmationDialog() {
        hideSeekBar();
        ConfirmationDialog.show(getActivity(), (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                Log.d(TAG, "Connection status: " + isConnected); // Log connection
                if (isConnected) {
                    currentNumber = Integer.parseInt((String) bindView.numberSpinner.getSelectedItem());
                    bindView.drawingView.saveImage(requireActivity());
                    sendDrawingToServer();
                    new Handler(Looper.getMainLooper()).postDelayed(this::resetDrawingViewAndIncreaseNumber, 200); // 200 milliseconds = 0.2 seconds

                } else {
                    Toast.makeText(getActivity(), "切断中のため、画像を送信できません", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendDrawingToServer() {
        Bitmap bitmap = bindView.drawingView.getBitmapFromDrawingView();
        if (bitmap != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int quality = 80;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            byte[] imageData = byteArrayOutputStream.toByteArray();
            connectToServer.sendImage(imageData);
        } else {
            Log.e(TAG, "Bitmap is null, cannot send to server");
        }
    }

    private void resetDrawingViewAndIncreaseNumber() {
        bindView.drawingView.clear();
        bindView.drawingView.resetUndoRedoStacks();
        currentNumber = Integer.parseInt((String) bindView.numberSpinner.getSelectedItem());
        if (currentNumber >= 99) {
            currentNumber = 90;
        } else {
            currentNumber++;
        }

        int position = currentNumber - 90;
        bindView.numberSpinner.setSelection(position);
    }

    private void selectPen() {
        Log.d(TAG, "Selecting pen");
        if (toolMode == TOOL_BLACK_PEN) {
            toolMode = TOOL_NEUTRAL;
            updateToolSelectionUI();
            viewPager.setSwipeEnabled(true);
            return;
        }
        toolMode = TOOL_BLACK_PEN;
        updateToolSelectionUI();
        bindView.drawingView.setToolMode(toolMode); // Activate drawing mode
        if (viewPager != null) {
            viewPager.setSwipeEnabled(false);
            startAnimation(bindView.ivPencil);
        }
    }

    private void selectEraser() {
        if (toolMode == TOOL_ERASER) {
            toolMode = TOOL_NEUTRAL;
            updateToolSelectionUI();
            viewPager.setSwipeEnabled(true);
            return;
        }
        toolMode = TOOL_ERASER;
        updateToolSelectionUI();
        bindView.drawingView.setToolMode(toolMode); // Activate the eraser mode
        if (viewPager != null) {
            viewPager.setSwipeEnabled(false);
            startAnimation(bindView.ivEraser);
        }
    }

    private void startAnimation(View view) {
        ScaleAnimation scaleUp = new ScaleAnimation(1f, 1.1f, 1f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleUp.setDuration(150);
        ScaleAnimation scaleDown = new ScaleAnimation(1.1f, 1f, 1.1f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleDown.setDuration(150);
        scaleDown.setStartOffset(150);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleUp);
        animationSet.addAnimation(scaleDown);
        view.startAnimation(animationSet);
    }

    private void updateToolSelectionUI() {
        bindView.ivPencil.setBackgroundColor(toolMode == TOOL_BLACK_PEN ? Color.LTGRAY : Color.TRANSPARENT);
        bindView.ivEraser.setBackgroundColor(toolMode == TOOL_ERASER ? Color.LTGRAY : Color.TRANSPARENT);
    }

    public void toggleMenus() {
        if (isMenuVisible) {
            bindView.topMenu.setVisibility(View.INVISIBLE);
            bindView.trBottomMenu.setVisibility(View.INVISIBLE);
        } else {
            bindView.topMenu.setVisibility(View.VISIBLE);
            bindView.trBottomMenu.setVisibility(View.VISIBLE);
        }
        isMenuVisible = !isMenuVisible;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateToolSelectionUI();
        // Log the connection status each time you resume the fragment
        Log.d("ConnectionStatus", "Is connected: " + isConnected);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public int isToolMode() {
        return toolMode;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectToServer.disconnect();
    }
}
