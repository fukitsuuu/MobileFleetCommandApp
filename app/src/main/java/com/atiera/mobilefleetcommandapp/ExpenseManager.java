package com.atiera.mobilefleetcommandapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseManager {
    
    private Context context;
    private List<Expense> expenses;
    private String currentTripId;
    private static int containerIdCounter = 1000;
    
    // UI Components
    private LinearLayout expensesArea;
    private AutoCompleteTextView expenseTypeDropdown;
    private TextInputEditText costAmountInput;
    private TextInputEditText fuelConsumptionInput;
    private ImageView uploadReceiptButton;
    private LinearLayout selectedImagesContainer;
    private Button addExpenseButton;
    private LinearLayout expensesListContainer;
    private LinearLayout totalExpensesContainer;
    private TextView totalExpensesText;
    private LinearLayout costAmountContainer;
    private LinearLayout fuelConsumptionContainer;
    private LinearLayout receiptImagesContainer;
    private LinearLayout descriptionContainer;
    private TextInputEditText descriptionInput;
    private LinearLayout serialNumberContainer;
    private TextInputEditText serialNumberInput;
    private LinearLayout invoiceDateContainer;
    private TextInputEditText invoiceDateInput;
    
    // Expense types
    private String[] expenseTypes = {"Fuel Cost"};
    
    // Image picker constants
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_CAMERA_PERMISSION = 1003;
    private List<String> selectedImagePaths = new ArrayList<>();
    private Uri cameraImageUri = null;
    private boolean isDropdownOpen = false;
    
    // Listener so screens (e.g., Dashboard) can react when expenses are added/removed
    public interface ExpensesChangedListener { void onExpensesChanged(boolean hasExpenses); }
    private ExpensesChangedListener expensesChangedListener;
    
    public ExpenseManager(Context context) {
        this.context = context;
        this.expenses = new ArrayList<>();
    }
    
    public void setupExpensesArea(View card, String tripId) {
        this.currentTripId = tripId;
        
        // Find UI components
        expensesArea = card.findViewById(R.id.expensesArea);
        android.util.Log.d("ExpenseManager", "setupExpensesArea called - expensesArea: " + (expensesArea != null) + ", visibility: " + (expensesArea != null ? expensesArea.getVisibility() : "null"));
        expenseTypeDropdown = card.findViewById(R.id.expenseTypeDropdown);
        costAmountInput = card.findViewById(R.id.costAmountInput);
        fuelConsumptionInput = card.findViewById(R.id.fuelConsumptionInput);
        uploadReceiptButton = card.findViewById(R.id.uploadReceiptButton);
        selectedImagesContainer = card.findViewById(R.id.selectedImagesContainer);
        addExpenseButton = card.findViewById(R.id.addExpenseButton);
        totalExpensesContainer = card.findViewById(R.id.totalExpensesContainer);
        totalExpensesText = card.findViewById(R.id.totalExpensesText);
        costAmountContainer = card.findViewById(R.id.costAmountContainer);
        fuelConsumptionContainer = card.findViewById(R.id.fuelConsumptionContainer);
        receiptImagesContainer = card.findViewById(R.id.receiptImagesContainer);
        descriptionContainer = card.findViewById(R.id.descriptionContainer);
        descriptionInput = card.findViewById(R.id.descriptionInput);
        serialNumberContainer = card.findViewById(R.id.serialNumberContainer);
        serialNumberInput = card.findViewById(R.id.serialNumberInput);
        invoiceDateContainer = card.findViewById(R.id.invoiceDateContainer);
        invoiceDateInput = card.findViewById(R.id.invoiceDateInput);
        
        // Debug: Check if description field is found
        android.util.Log.d("ExpenseManager", "Description field found - container: " + (descriptionContainer != null) + ", input: " + (descriptionInput != null));
        
        // Debug logging
        android.util.Log.d("ExpenseManager", "UI Components found - selectedImagesContainer: " + (selectedImagesContainer != null) + 
                          ", uploadReceiptButton: " + (uploadReceiptButton != null) + 
                          ", descriptionContainer: " + (descriptionContainer != null));
        
        // Debug: Check if container is found
        if (selectedImagesContainer == null) {
            android.util.Log.e("ExpenseManager", "selectedImagesContainer is NULL during setup!");
        }
        
        if (expensesArea == null) return;
        
        // Setup dropdown
        setupDropdown();
        
        // Setup image picker
        setupImagePicker();
        
        // Setup add expense button
        setupAddExpenseButton();
        
        // Setup cost amount input styling
        setupCostAmountInput();

        // Setup invoice date picker (date + time)
        if (invoiceDateInput != null) {
            invoiceDateInput.setFocusable(false);
            invoiceDateInput.setClickable(true);
            invoiceDateInput.setOnClickListener(v -> showInvoiceDateTimePicker());
        }
        
        // Create expenses list container if it doesn't exist
        createExpensesListContainer();
        
        // Ensure expenses area is visible
        if (expensesArea != null) {
            expensesArea.setVisibility(View.VISIBLE);
            android.util.Log.d("ExpenseManager", "Expenses area set to visible");
        }
        
        // Initially hide fields that are only for certain expense types
        if (descriptionContainer != null) {
            descriptionContainer.setVisibility(View.GONE);
        }
        if (serialNumberContainer != null) {
            serialNumberContainer.setVisibility(View.GONE);
        }
        if (invoiceDateContainer != null) {
            invoiceDateContainer.setVisibility(View.GONE);
        }
        
        // Check if there's already a selected expense type and adjust description visibility
        if (expenseTypeDropdown != null && expenseTypeDropdown.getText() != null) {
            String currentType = expenseTypeDropdown.getText().toString();
            if (!currentType.isEmpty() && !"Select expense type".equals(currentType)) {
                showDescriptionField(currentType);
            }
        }
        
        // Initialize add button enabled state
        updateAddButtonEnabled();
        // Notify initial empty state
        notifyExpensesChanged();
        // Notify initial empty state
        notifyExpensesChanged();
    }
    
    private void setupDropdown() {
        if (expenseTypeDropdown != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
                R.layout.dropdown_item, expenseTypes);
            expenseTypeDropdown.setAdapter(adapter);
            
            // Set dropdown background
            expenseTypeDropdown.setDropDownBackgroundDrawable(
                context.getResources().getDrawable(R.drawable.dropdown_popup_background)
            );
            
            // Start with empty selection
            expenseTypeDropdown.setText("", false);
            expenseTypeDropdown.setHint("Select expense type");
            expenseTypeDropdown.setTextColor(0xFF9CA3AF); // Light grey for hint text
            
            // Enable dropdown functionality
            expenseTypeDropdown.setThreshold(0); // Show all items immediately
            
            // Disable default click behavior and handle manually
            expenseTypeDropdown.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    if (expenseTypeDropdown.getAdapter() != null) {
                        android.util.Log.d("ExpenseManager", "Dropdown clicked - isDropdownOpen: " + isDropdownOpen);
                        
                        if (isDropdownOpen) {
                            android.util.Log.d("ExpenseManager", "Dismissing dropdown");
                            expenseTypeDropdown.dismissDropDown();
                            isDropdownOpen = false;
                        } else {
                            android.util.Log.d("ExpenseManager", "Showing dropdown");
                            expenseTypeDropdown.showDropDown();
                            isDropdownOpen = true;
                        }
                    }
                    return true; // Consume the event
                }
                return false;
            });
            
            // Disable focus listener to prevent auto-opening
            expenseTypeDropdown.setOnFocusChangeListener(null);
            
            // Handle item selection
            expenseTypeDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selectedType = expenseTypes[position];
                expenseTypeDropdown.setText(selectedType, false);
                // Ensure the text color is bright and bold
                expenseTypeDropdown.setTextColor(0xFF000000); // Black color for maximum visibility
                isDropdownOpen = false; // Reset flag when item is selected
                
                // Show cost amount and receipt images sections when expense type is selected
                showExpenseInputSections();
                
                // Show/hide description field based on expense type
                showDescriptionField(selectedType);
                
                updateAddButtonEnabled();
            });
        }
    }
    
    private void showExpenseInputSections() {
        // Show cost amount container
        if (costAmountContainer != null) {
            costAmountContainer.setVisibility(View.VISIBLE);
        }
        
        // Show fuel consumption container (for Fuel Cost)
        if (fuelConsumptionContainer != null) {
            fuelConsumptionContainer.setVisibility(View.VISIBLE);
        }
        
        // Show receipt images container
        if (receiptImagesContainer != null) {
            receiptImagesContainer.setVisibility(View.VISIBLE);
        }
        
        android.util.Log.d("ExpenseManager", "Expense input sections shown");
    }
    
    private void showDescriptionField(String expenseType) {
        boolean show = "Fuel Cost".equalsIgnoreCase(expenseType);

        if (descriptionContainer != null) {
            descriptionContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show && descriptionInput != null) {
                descriptionInput.setText("");
            }
        }
        if (serialNumberContainer != null) {
            serialNumberContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show && serialNumberInput != null) {
                serialNumberInput.setText("");
            }
        }
        if (invoiceDateContainer != null) {
            invoiceDateContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show && invoiceDateInput != null) {
                invoiceDateInput.setText("");
            }
        }
    }
    
    private void setupImagePicker() {
        if (uploadReceiptButton != null) {
            uploadReceiptButton.setOnClickListener(v -> {
                android.util.Log.d("ExpenseManager", "Upload button clicked! Current images: " + selectedImagePaths.size());
                if (selectedImagePaths.size() >= 5) {
                    Toast.makeText(context, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show();
                    return;
                }
                showImagePickerOptions();
            });
        } else {
            android.util.Log.e("ExpenseManager", "Upload button is null!");
        }
    }
    
    private void showImagePickerOptions() {
        // Check and request media permissions first
        if (!checkMediaPermissions()) {
            requestMediaPermissions();
            return;
        }
        
        // Create multiple intents to ensure all gallery apps appear
        List<Intent> intents = new ArrayList<>();
        
        // Intent 1: ACTION_GET_CONTENT (shows most gallery apps)
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        intents.add(getContentIntent);
        
        // Intent 2: ACTION_PICK (alternative gallery picker)
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        intents.add(pickIntent);
        
        // Intent 3: Camera (if permission granted)
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intents.add(cameraIntent);
        } else {
            // Request camera permission but still show gallery options
            ActivityCompat.requestPermissions((Activity) context, 
                new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        
        // Create chooser with all intents
        Intent chooserIntent = Intent.createChooser(intents.get(0), "Select Receipt Image");
        if (intents.size() > 1) {
            Intent[] extraIntents = intents.subList(1, intents.size()).toArray(new Intent[0]);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
        }
        
        if (context instanceof Activity) {
            try {
                ((Activity) context).startActivityForResult(chooserIntent, REQUEST_IMAGE_PICK);
            } catch (Exception e) {
                Toast.makeText(context, "Error opening image picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "Cannot open image picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean checkMediaPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - use new media permissions
            return ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_IMAGES") 
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 and below - use legacy storage permissions
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestMediaPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - request new media permissions
            ActivityCompat.requestPermissions((Activity) context, 
                new String[]{"android.permission.READ_MEDIA_IMAGES"}, REQUEST_CAMERA_PERMISSION);
        } else {
            // Android 12 and below - request legacy storage permissions
            ActivityCompat.requestPermissions((Activity) context, 
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        }
    }
    
    
    private File createImageFile() {
        try {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = context.getExternalFilesDir("Pictures");
            if (storageDir != null) {
                File image = File.createTempFile(imageFileName, ".jpg", storageDir);
                return image;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
    
    public void handleImagePickerResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        
        if (requestCode == REQUEST_IMAGE_PICK) {
            Bitmap bitmap = null;
            String imagePath = null;
            
            // First try to get thumbnail from camera (most reliable)
            if (data != null && data.getExtras() != null) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                if (thumbnail != null) {
                    bitmap = thumbnail;
                    imagePath = saveImageToInternalStorage(thumbnail);
                    Toast.makeText(context, "Image captured successfully", Toast.LENGTH_SHORT).show();
                }
            }
            
            // If no thumbnail, try to get URI from gallery
            if (bitmap == null && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        // Load the selected image
                        bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), selectedImageUri);
                        imagePath = saveImageToInternalStorage(bitmap);
                        Toast.makeText(context, "Image selected successfully", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // Add image to list and display
            android.util.Log.d("ExpenseManager", "Processing image result - bitmap: " + (bitmap != null) + ", imagePath: " + imagePath);
            if (bitmap != null && imagePath != null) {
                android.util.Log.d("ExpenseManager", "Image selected successfully. Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                selectedImagePaths.add(imagePath);
                android.util.Log.d("ExpenseManager", "About to call addImageToContainer");
                addImageToContainer(bitmap, imagePath);
                android.util.Log.d("ExpenseManager", "addImageToContainer called");
            } else {
                android.util.Log.d("ExpenseManager", "Failed to get image - bitmap: " + (bitmap != null) + ", imagePath: " + imagePath);
            }
        }
    }
    
    
    private void addImageToContainer(Bitmap bitmap, String imagePath) {
        if (selectedImagesContainer == null) {
            android.util.Log.e("ExpenseManager", "selectedImagesContainer is null!");
            return;
        }
        
        android.util.Log.d("ExpenseManager", "Adding image to container. Container child count before: " + selectedImagesContainer.getChildCount());
        
        // Debug: Log that we're adding image to container
        android.util.Log.d("ExpenseManager", "Adding image to container - count: " + selectedImagePaths.size());
        
        // Convert dp to pixels
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (80 * density); // 80dp to pixels
        int marginPx = (int) (8 * density); // 8dp to pixels
        
        // Debug: Log the pixel values
        android.util.Log.d("ExpenseManager", "Density: " + density + ", SizePx: " + sizePx + ", MarginPx: " + marginPx);
        
        // Create a FrameLayout container - EXACTLY 80x80dp in pixels
        android.widget.FrameLayout imageContainer = new android.widget.FrameLayout(context);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(sizePx, sizePx);
        containerParams.setMargins(0, 0, marginPx, 0);
        imageContainer.setLayoutParams(containerParams);
        
        // FORCE resize bitmap to exactly 80x80dp in pixels
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true);
        
        // Create image view - EXACTLY 80x80dp in pixels, NO EXCEPTIONS
        ImageView imageView = new ImageView(context);
        android.widget.FrameLayout.LayoutParams imageParams = new android.widget.FrameLayout.LayoutParams(sizePx, sizePx);
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY); // Force exact fit
        imageView.setImageBitmap(resizedBitmap);
        // imageView.setBackgroundResource(R.drawable.image_background);
        imageView.setPadding(0, 0, 0, 0);
        imageView.setAdjustViewBounds(false);
        imageView.setMinimumWidth(sizePx);
        imageView.setMinimumHeight(sizePx);
        imageView.setMaxWidth(sizePx);
        imageView.setMaxHeight(sizePx);
        // Tap to preview full screen
        imageView.setOnClickListener(v -> showImagePreview(imagePath));
        
        // Debug: Log actual dimensions after layout
        imageView.post(() -> {
            android.util.Log.d("ExpenseManager", "Image view actual size: " + imageView.getWidth() + "x" + imageView.getHeight());
            android.util.Log.d("ExpenseManager", "Image view measured size: " + imageView.getMeasuredWidth() + "x" + imageView.getMeasuredHeight());
        });
        
        // Create remove button
        ImageView removeButton = new ImageView(context);
        int removeButtonSizePx = (int) (28 * density); // 28dp to pixels
        android.widget.FrameLayout.LayoutParams removeParams = new android.widget.FrameLayout.LayoutParams(removeButtonSizePx, removeButtonSizePx);
        removeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        removeParams.setMargins(0, (int)(2 * density), (int)(2 * density), 0);
        removeButton.setLayoutParams(removeParams);
        removeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeButton.setBackgroundColor(0xCCFF0000);
        removeButton.setPadding(4, 4, 4, 4);
        removeButton.setColorFilter(0xFFFFFFFF);
        removeButton.setClickable(true);
        
        // Add views to container
        imageContainer.addView(imageView);
        imageContainer.addView(removeButton);
        
        // Set up remove functionality
        removeButton.setOnClickListener(v -> {
            selectedImagePaths.remove(imagePath);
            selectedImagesContainer.removeView(imageContainer);
        });
        
        selectedImagesContainer.addView(imageContainer);
        
        // Debug log
        android.util.Log.d("ExpenseManager", "Added image to container. Total images: " + selectedImagePaths.size() + 
                          ", Container child count after: " + selectedImagesContainer.getChildCount());
        
        // Force refresh the container
        selectedImagesContainer.invalidate();
        selectedImagesContainer.requestLayout();

        // After successfully adding an image, try to analyze the receipt with AI
        analyzeReceiptWithAI(bitmap);
    }

    private void showImagePreview(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                Toast.makeText(context, "Unable to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Root overlay fills the whole screen with semi-transparent gray
            android.widget.FrameLayout root = new android.widget.FrameLayout(context);
            root.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ));
            root.setBackgroundColor(0x99000000); // semi-transparent gray
            root.setClickable(true);
            root.setFocusable(true);

            // Centered preview ImageView (wraps content; outside remains clickable)
            ImageView preview = new ImageView(context);
            preview.setImageBitmap(bitmap);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setAdjustViewBounds(true);

            int screenW = context.getResources().getDisplayMetrics().widthPixels;
            int screenH = context.getResources().getDisplayMetrics().heightPixels;
            int maxW = (int)(screenW * 0.9f);
            int maxH = (int)(screenH * 0.9f);
            preview.setMaxWidth(maxW);
            preview.setMaxHeight(maxH);

            android.widget.FrameLayout.LayoutParams centerParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            centerParams.gravity = android.view.Gravity.CENTER;
            root.addView(preview, centerParams);

            // Fullscreen dialog
            android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(root);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            // Close when tapping outside the image
            root.setOnClickListener(v -> dialog.dismiss());
            // Consume taps on the image so it doesn't dismiss
            preview.setClickable(true);
            preview.setOnClickListener(v -> {});

            // Show
            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                );
                dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                );
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error showing preview", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String saveImageToInternalStorage(Bitmap bitmap) {
        try {
            // Create a file in the app's internal storage
            File file = new File(context.getFilesDir(), "receipt_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }
    
    private void setupAddExpenseButton() {
        if (addExpenseButton != null) {
            addExpenseButton.setOnClickListener(v -> addExpense());
            // Make sure the button is visible
            addExpenseButton.setVisibility(View.VISIBLE);
            // Update button text based on current state
            updateAddExpenseButtonText();
        }
    }
    
    private void updateAddExpenseButtonText() {
        if (addExpenseButton != null) {
            if (hasExpenses()) {
                addExpenseButton.setText("Add Expenses");
            } else {
                addExpenseButton.setText("Add Expenses");
            }
        }
    }
    
    private void setupCostAmountInput() {
        if (costAmountInput != null) {
            // Set initial text color
            costAmountInput.setTextColor(0xFF9CA3AF); // Light grey color
            
            // Add text watcher to maintain light color and enable button when valid
            costAmountInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // Keep the light grey color
                    costAmountInput.setTextColor(0xFF9CA3AF);
                    updateAddButtonEnabled();
                }
            });
        }
    }
    
    private void createExpensesListContainer() {
        // Check if expenses list container already exists
        int containerId = containerIdCounter++;
        expensesListContainer = expensesArea.findViewById(containerId);
        
        if (expensesListContainer == null) {
            // Create expenses list container
            expensesListContainer = new LinearLayout(context);
            expensesListContainer.setId(containerId);
            expensesListContainer.setOrientation(LinearLayout.VERTICAL);
            expensesListContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            // Insert before the add expense button
            int buttonIndex = expensesArea.indexOfChild(addExpenseButton);
            expensesArea.addView(expensesListContainer, buttonIndex);
        }
    }
    
    private void addExpense() {
        if (!validateInputs()) return;
        
        String expenseType = expenseTypeDropdown.getText().toString();
        String amountText = costAmountInput.getText().toString();
        double amount = Double.parseDouble(amountText);
        
        // Get fuel consumption if available
        double fuelConsumption = 0.0;
        if (fuelConsumptionInput != null) {
            String fuelConsumptionText = fuelConsumptionInput.getText().toString().trim();
            android.util.Log.d("ExpenseManager", "Fuel consumption input text: '" + fuelConsumptionText + "'");
            if (!fuelConsumptionText.isEmpty()) {
                try {
                    fuelConsumption = Double.parseDouble(fuelConsumptionText);
                    android.util.Log.d("ExpenseManager", "Fuel consumption parsed: " + fuelConsumption);
                } catch (NumberFormatException e) {
                    android.util.Log.e("ExpenseManager", "Failed to parse fuel consumption: " + e.getMessage());
                    fuelConsumption = 0.0;
                }
            } else {
                android.util.Log.d("ExpenseManager", "Fuel consumption input is empty");
            }
        } else {
            android.util.Log.e("ExpenseManager", "fuelConsumptionInput is null!");
        }
        
        // Get description if available
        String description = "";
        if (descriptionInput != null) {
            description = descriptionInput.getText().toString().trim();
        }
        String serialNumber = "";
        if (serialNumberInput != null) {
            serialNumber = serialNumberInput.getText().toString().trim();
        }
        String invoiceDate = "";
        if (invoiceDateInput != null) {
            invoiceDate = invoiceDateInput.getText().toString().trim();
        }
        
        // Create new expense with image paths (comma-separated)
        String imagePaths = String.join(",", selectedImagePaths);
        Expense expense = new Expense(currentTripId, expenseType, amount, fuelConsumption, imagePaths, description, serialNumber, invoiceDate);
        expenses.add(expense);
        
        // Add to UI (do NOT post yet; posting happens on Mark as done)
        addExpenseToUI(expense);
        
        // Update total display
        updateTotalDisplay();
        
        // Update button text
        updateAddExpenseButtonText();
        
        // Clear form for the next expense (increment form)
        clearForm();
        updateAddButtonEnabled();
        // Notify listeners that we now have at least one expense
        notifyExpensesChanged();
        
        // Show success message
        Toast.makeText(context, "Expense added: " + expense.getFormattedType() + " - " + expense.getFormattedAmount(), Toast.LENGTH_SHORT).show();
    }

    public void submitAllExpensesForTrip(SubmissionCallback callback) {
        // Build a list of expenses to submit (include current filled form if any)
        List<Expense> toSubmit = new ArrayList<>();
        // Include already added ones
        toSubmit.addAll(expenses);
        // Include current form if populated
        if (isFormPopulated()) {
            String type = expenseTypeDropdown.getText().toString();
            String amountText = costAmountInput.getText().toString();
            try {
                double amount = Double.parseDouble(amountText);
                double fuelConsumption = 0.0;
                if (fuelConsumptionInput != null) {
                    String fuelConsumptionText = fuelConsumptionInput.getText().toString().trim();
                    android.util.Log.d("ExpenseManager", "submitAllExpensesForTrip - Fuel consumption input text: '" + fuelConsumptionText + "'");
                    if (!fuelConsumptionText.isEmpty()) {
                        try {
                            fuelConsumption = Double.parseDouble(fuelConsumptionText);
                            android.util.Log.d("ExpenseManager", "submitAllExpensesForTrip - Fuel consumption parsed: " + fuelConsumption);
                        } catch (NumberFormatException e) {
                            android.util.Log.e("ExpenseManager", "submitAllExpensesForTrip - Failed to parse fuel consumption: " + e.getMessage());
                            fuelConsumption = 0.0;
                        }
                    } else {
                        android.util.Log.d("ExpenseManager", "submitAllExpensesForTrip - Fuel consumption input is empty");
                    }
                } else {
                    android.util.Log.e("ExpenseManager", "submitAllExpensesForTrip - fuelConsumptionInput is null!");
                }
                String imagePaths = String.join(",", selectedImagePaths);
                String description = "";
                if (descriptionInput != null) {
                    description = descriptionInput.getText().toString().trim();
                }
                String serialNumber = "";
                if (serialNumberInput != null) {
                    serialNumber = serialNumberInput.getText().toString().trim();
                }
                String invoiceDate = "";
                if (invoiceDateInput != null) {
                    invoiceDate = invoiceDateInput.getText().toString().trim();
                }
                android.util.Log.d("ExpenseManager", "submitAllExpensesForTrip - Creating expense with fuel_consumption: " + fuelConsumption);
                toSubmit.add(new Expense(currentTripId, type, amount, fuelConsumption, imagePaths, description, serialNumber, invoiceDate));
            } catch (Exception ignored) {}
        }
        
        // Validate that at least one expense has a receipt image
        boolean hasReceiptImage = false;
        for (Expense expense : toSubmit) {
            String receiptPath = expense.getReceiptImagePath();
            if (receiptPath != null && !receiptPath.trim().isEmpty()) {
                hasReceiptImage = true;
                break;
            }
        }
        
        // If no expenses or no receipt images, show error and return
        if (toSubmit.isEmpty()) {
            if (callback != null) callback.onResult(true);
            return;
        }
        
        if (!hasReceiptImage) {
            Toast.makeText(context, "Please upload at least one receipt image before marking as done", Toast.LENGTH_LONG).show();
            if (callback != null) callback.onResult(false);
            return;
        }
        // Submit all, wait for all to finish
        final int[] remaining = { toSubmit.size() };
        final boolean[] anyFail = { false };
        for (Expense e : toSubmit) {
            postFuelExpense(e, success -> {
                if (!success) anyFail[0] = true;
                remaining[0] -= 1;
                if (remaining[0] == 0) {
                    // On success, clear current form (if any) and keep local list as-is
                    if (isFormPopulated()) {
                        clearForm();
                        updateAddButtonEnabled();
                    }
                    if (callback != null) callback.onResult(!anyFail[0]);
                }
            });
        }
    }

    private void postFuelExpense(Expense expense) {
        // Simple background thread using Java thread (no external libs)
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(BuildConfig.BASE_URL + "add_expense.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("trip_id", expense.getTripId());
                payload.put("expense_type", expense.getExpenseType());
                payload.put("amount", expense.getAmount());
                double fuelConsumptionValue = expense.getFuelConsumption();
                payload.put("fuel_consumption", fuelConsumptionValue);
                android.util.Log.d("ExpenseManager", "Sending fuel_consumption (non-callback): " + fuelConsumptionValue + " for trip: " + expense.getTripId());
                payload.put("description", expense.getDescription());
                // New fields for receipt serial number and invoice date
                payload.put("serial_number", expense.getSerialNumber());
                payload.put("invoice_date", expense.getInvoiceDate());
                // Attach base64 contents of all selected images
                org.json.JSONArray imagesArray = new org.json.JSONArray();
                try {
                    String allPaths = expense.getReceiptImagePath();
                    android.util.Log.d("ExpenseManager", "Processing receipt images (non-callback), allPaths: " + (allPaths != null ? allPaths : "null"));
                    if (allPaths != null && !allPaths.isEmpty()) {
                        String[] parts = allPaths.split(",");
                        android.util.Log.d("ExpenseManager", "Found " + parts.length + " image path(s)");
                        int imageIndex = 0;
                        for (String p : parts) {
                            imageIndex++;
                            String path = p.trim();
                            if (path.isEmpty()) {
                                android.util.Log.w("ExpenseManager", "Skipping empty path at index " + imageIndex);
                                continue;
                            }
                            java.io.File f = new java.io.File(path);
                            if (f.exists()) {
                                try {
                                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                                    android.util.Log.d("ExpenseManager", "Image #" + imageIndex + " read successfully: " + bytes.length + " bytes from " + path);
                                    String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                                    imagesArray.put("data:image/jpeg;base64," + b64);
                                    android.util.Log.d("ExpenseManager", "Image #" + imageIndex + " added to array, base64 length: " + b64.length());
                                } catch (Exception e) {
                                    android.util.Log.e("ExpenseManager", "Error reading image #" + imageIndex + " from " + path + ": " + e.getMessage());
                                }
                            } else {
                                android.util.Log.w("ExpenseManager", "Image file does not exist: " + path);
                            }
                        }
                    }
                    android.util.Log.d("ExpenseManager", "Total images in array (non-callback): " + imagesArray.length());
                } catch (Exception e) {
                    android.util.Log.e("ExpenseManager", "Error processing receipt images (non-callback): " + e.getMessage());
                    e.printStackTrace();
                }
                payload.put("receipt_images_base64", imagesArray);

                byte[] body = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(body); }

                int code = conn.getResponseCode();
                java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String resp = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                conn.disconnect();

                android.util.Log.d("ExpenseManager", "postFuelExpense resp(" + code + "): " + resp);
            } catch (Exception e) {
                android.util.Log.e("ExpenseManager", "postFuelExpense error: " + e.getMessage());
            }
        }).start();
    }

    private void postFuelExpense(Expense expense, SubmissionCallback callback) {
        new Thread(() -> {
            boolean success = false;
            try {
                java.net.URL url = new java.net.URL(BuildConfig.BASE_URL + "add_expense.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("trip_id", expense.getTripId());
                payload.put("expense_type", expense.getExpenseType());
                payload.put("amount", expense.getAmount());
                double fuelConsumptionValue = expense.getFuelConsumption();
                payload.put("fuel_consumption", fuelConsumptionValue);
                android.util.Log.d("ExpenseManager", "Sending fuel_consumption (callback): " + fuelConsumptionValue + " for trip: " + expense.getTripId());
                payload.put("description", expense.getDescription());
                // New fields for receipt serial number and invoice date
                payload.put("serial_number", expense.getSerialNumber());
                payload.put("invoice_date", expense.getInvoiceDate());

                // Build base64 images array (same as non-callback method) so server saves to database/receipts
                org.json.JSONArray imagesArray = new org.json.JSONArray();
                try {
                    String allPaths = expense.getReceiptImagePath();
                    android.util.Log.d("ExpenseManager", "Processing receipt images, allPaths: " + (allPaths != null ? allPaths : "null"));
                    if (allPaths != null && !allPaths.isEmpty()) {
                        String[] parts = allPaths.split(",");
                        android.util.Log.d("ExpenseManager", "Found " + parts.length + " image path(s)");
                        int imageIndex = 0;
                        for (String p : parts) {
                            imageIndex++;
                            String path = p.trim();
                            if (path.isEmpty()) {
                                android.util.Log.w("ExpenseManager", "Skipping empty path at index " + imageIndex);
                                continue;
                            }
                            java.io.File f = new java.io.File(path);
                            if (f.exists()) {
                                try {
                                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                                    android.util.Log.d("ExpenseManager", "Image #" + imageIndex + " read successfully: " + bytes.length + " bytes from " + path);
                                    String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                                    imagesArray.put("data:image/jpeg;base64," + b64);
                                    android.util.Log.d("ExpenseManager", "Image #" + imageIndex + " added to array, base64 length: " + b64.length());
                                } catch (Exception e) {
                                    android.util.Log.e("ExpenseManager", "Error reading image #" + imageIndex + " from " + path + ": " + e.getMessage());
                                }
                            } else {
                                android.util.Log.w("ExpenseManager", "Image file does not exist: " + path);
                            }
                        }
                    }
                    android.util.Log.d("ExpenseManager", "Total images in array: " + imagesArray.length());
                } catch (Exception e) {
                    android.util.Log.e("ExpenseManager", "Error processing receipt images: " + e.getMessage());
                    e.printStackTrace();
                }
                payload.put("receipt_images_base64", imagesArray);
                // Do NOT send the local Android file path to the server

                byte[] body = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(body); }

                int code = conn.getResponseCode();
                java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String resp = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                conn.disconnect();
                success = (code >= 200 && code < 300);
                android.util.Log.d("ExpenseManager", "postFuelExpense(cb) resp(" + code + "): " + resp);
            } catch (Exception e) {
                android.util.Log.e("ExpenseManager", "postFuelExpense(cb) error: " + e.getMessage());
            }
            boolean finalSuccess = success;
            // switch back to UI thread
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (callback != null) callback.onResult(finalSuccess);
                });
            } else {
                if (callback != null) callback.onResult(finalSuccess);
            }
        }).start();
    }
    
    private boolean validateInputs() {
        if (expenseTypeDropdown == null || costAmountInput == null) return false;
        
        String expenseType = expenseTypeDropdown.getText().toString();
        String amountText = costAmountInput.getText().toString();
        
        if (expenseType.isEmpty()) {
            Toast.makeText(context, "Please select an expense type", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (amountText.isEmpty()) {
            Toast.makeText(context, "Please enter the cost amount", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                Toast.makeText(context, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    // Lightweight validation used only to enable/disable the Add button (no toasts)
    private boolean isFormPopulated() {
        if (expenseTypeDropdown == null || costAmountInput == null) return false;
        String expenseType = expenseTypeDropdown.getText().toString();
        String amountText = costAmountInput.getText().toString();
        if (expenseType.isEmpty()) return false;
        try {
            double amount = Double.parseDouble(amountText);
            return amount > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateAddButtonEnabled() {
        boolean enabled = isFormPopulated();
        if (addExpenseButton != null) {
            addExpenseButton.setEnabled(enabled);
            addExpenseButton.setAlpha(enabled ? 1f : 0.5f);
        }
    }
    
    private void addExpenseToUI(Expense expense) {
        if (expensesListContainer == null) return;
        
        // Create expense item view
        View expenseItem = createExpenseItemView(expense);
        expensesListContainer.addView(expenseItem);
    }
    
    private View createExpenseItemView(Expense expense) {
        LinearLayout expenseItem = new LinearLayout(context);
        expenseItem.setOrientation(LinearLayout.HORIZONTAL);
        expenseItem.setPadding(16, 12, 16, 12);
        expenseItem.setBackgroundColor(0xFFF3F4F6);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        expenseItem.setLayoutParams(params);
        
        // Expense type and amount
        LinearLayout expenseInfo = new LinearLayout(context);
        expenseInfo.setOrientation(LinearLayout.VERTICAL);
        expenseInfo.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        ));
        
        TextView typeText = new TextView(context);
        typeText.setText(expense.getFormattedType());
        typeText.setTextColor(0xFF374151);
        typeText.setTextSize(14);
        typeText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView amountText = new TextView(context);
        amountText.setText(expense.getFormattedAmount());
        amountText.setTextColor(0xFF059669);
        amountText.setTextSize(16);
        amountText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        expenseInfo.addView(typeText);
        expenseInfo.addView(amountText);
        
        // Remove button
        Button removeButton = new Button(context);
        removeButton.setText("Remove");
        removeButton.setTextColor(0xFFDC2626);
        removeButton.setBackgroundColor(0x00FFFFFF);
        removeButton.setTextSize(12);
        
        removeButton.setOnClickListener(v -> {
            expenses.remove(expense);
            expensesListContainer.removeView(expenseItem);
            updateTotalDisplay();
            updateAddExpenseButtonText();
            // Notify listeners after removal
            notifyExpensesChanged();
            Toast.makeText(context, "Expense removed", Toast.LENGTH_SHORT).show();
        });
        
        expenseItem.addView(expenseInfo);
        expenseItem.addView(removeButton);
        
        return expenseItem;
    }
    
    private void clearForm() {
        if (expenseTypeDropdown != null) {
            expenseTypeDropdown.setText("", false);
            expenseTypeDropdown.setTextColor(0xFF9CA3AF); // Light grey for hint text
        }
        if (costAmountInput != null) {
            costAmountInput.setText("");
            costAmountInput.setTextColor(0xFF9CA3AF); // Light grey color
        }
        if (fuelConsumptionInput != null) {
            fuelConsumptionInput.setText("");
        }
        if (descriptionInput != null) { descriptionInput.setText(""); }
        if (serialNumberInput != null) { serialNumberInput.setText(""); }
        if (invoiceDateInput != null) { invoiceDateInput.setText(""); }
        if (selectedImagesContainer != null) {
            selectedImagesContainer.removeAllViews();
        }
        selectedImagePaths.clear(); // Clear all selected images
        
        // Hide description field after clearing
        if (descriptionContainer != null) {
            descriptionContainer.setVisibility(View.GONE);
        }
    }
    
    public List<Expense> getExpenses() {
        return expenses;
    }
    
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("ExpenseManager", "Media permission granted, retrying image picker");
                showImagePickerOptions();
            } else {
                Toast.makeText(context, "Permission denied. Cannot access images.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, show image picker again with camera included
                showImagePickerOptions();
            } else {
                // Permission denied, show message but gallery is still available
                Toast.makeText(context, "Camera permission denied. You can still use gallery to select photos.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    public double getTotalAmount() {
        double total = 0;
        for (Expense expense : expenses) {
            total += expense.getAmount();
        }
        return total;
    }
    
    public String getFormattedTotal() {
        return "₱" + String.format("%.2f", getTotalAmount());
    }
    
    public boolean hasExpenses() {
        return !expenses.isEmpty();
    }

    // Allow callers to react to expense list changes (e.g., enable/disable buttons)
    public void setExpensesChangedListener(ExpensesChangedListener listener) {
        this.expensesChangedListener = listener;
        notifyExpensesChanged();
    }

    private void notifyExpensesChanged() {
        if (expensesChangedListener != null) {
            expensesChangedListener.onExpensesChanged(hasExpenses());
        }
    }

    private void updateTotalDisplay() {
        if (totalExpensesContainer == null || totalExpensesText == null) return;
        
        if (hasExpenses()) {
            totalExpensesContainer.setVisibility(View.VISIBLE);
            totalExpensesText.setText(getFormattedTotal());
        } else {
            totalExpensesContainer.setVisibility(View.GONE);
        }
    }

    public void submitPendingExpensesIfAny() {
        if (!isFormPopulated()) return;
        String expenseType = expenseTypeDropdown.getText().toString();
        String amountText = costAmountInput.getText().toString();
        double amount;
        try { amount = Double.parseDouble(amountText); } catch (Exception e) { return; }
        double fuelConsumption = 0.0;
        if (fuelConsumptionInput != null) {
            String fuelConsumptionText = fuelConsumptionInput.getText().toString().trim();
            if (!fuelConsumptionText.isEmpty()) {
                try {
                    fuelConsumption = Double.parseDouble(fuelConsumptionText);
                } catch (NumberFormatException e) {
                    fuelConsumption = 0.0;
                }
            }
        }
        String imagePaths = String.join(",", selectedImagePaths);
        String description = "";
        if (descriptionInput != null) {
            description = descriptionInput.getText().toString().trim();
        }
        String serialNumber = "";
        if (serialNumberInput != null) {
            serialNumber = serialNumberInput.getText().toString().trim();
        }
        String invoiceDate = "";
        if (invoiceDateInput != null) {
            invoiceDate = invoiceDateInput.getText().toString().trim();
        }
        Expense expense = new Expense(currentTripId, expenseType, amount, fuelConsumption, imagePaths, description, serialNumber, invoiceDate);
        expenses.add(expense);
        addExpenseToUI(expense);
        // Submit all expense types
        postFuelExpense(expense);
        clearForm();
        updateAddButtonEnabled();
    }

    public interface SubmissionCallback { void onResult(boolean success); }

    public void submitPendingFuelExpenseIfAny(SubmissionCallback callback) {
        if (!isFormPopulated()) {
            if (callback != null) callback.onResult(true);
            return;
        }
        String type = expenseTypeDropdown.getText().toString();
        String amountText = costAmountInput.getText().toString();
        double amount;
        try { amount = Double.parseDouble(amountText); } catch (Exception e) { if (callback != null) callback.onResult(false); return; }
        double fuelConsumption = 0.0;
        if (fuelConsumptionInput != null) {
            String fuelConsumptionText = fuelConsumptionInput.getText().toString().trim();
            if (!fuelConsumptionText.isEmpty()) {
                try {
                    fuelConsumption = Double.parseDouble(fuelConsumptionText);
                } catch (NumberFormatException e) {
                    fuelConsumption = 0.0;
                }
            }
        }
        String imagePaths = String.join(",", selectedImagePaths);
        String description = "";
        if (descriptionInput != null) {
            description = descriptionInput.getText().toString().trim();
        }
        String serialNumber = "";
        if (serialNumberInput != null) {
            serialNumber = serialNumberInput.getText().toString().trim();
        }
        String invoiceDate = "";
        if (invoiceDateInput != null) {
            invoiceDate = invoiceDateInput.getText().toString().trim();
        }
        Expense expense = new Expense(currentTripId, type, amount, fuelConsumption, imagePaths, description, serialNumber, invoiceDate);
        // Try posting; only proceed on success
        postFuelExpense(expense, success -> {
            if (success) {
                // reflect in UI
                expenses.add(expense);
                addExpenseToUI(expense);
                clearForm();
                updateAddButtonEnabled();
            }
            if (callback != null) callback.onResult(success);
        });
    }

    // ---- AI-powered receipt analysis ----

    private void analyzeReceiptWithAI(Bitmap bitmap) {
        if (bitmap == null) return;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] bytes = baos.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

            ApiClient.get().analyzeReceipt(base64).enqueue(new Callback<ReceiptAnalysisResponse>() {
                @Override
                public void onResponse(Call<ReceiptAnalysisResponse> call, Response<ReceiptAnalysisResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(context, "Could not analyze receipt (server error)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ReceiptAnalysisResponse body = response.body();
                    if (!body.ok) {
                        // Only show a toast if backend explicitly reports an error
                        if (body.msg != null && !body.msg.isEmpty()) {
                            Toast.makeText(context, body.msg, Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    // Auto-fill amount if detected
                    if (body.amount != null && costAmountInput != null) {
                        costAmountInput.setText(String.format(java.util.Locale.US, "%.2f", body.amount));
                    }

                    // Auto-fill fuel liters if detected
                    if (body.fuelLiters != null && fuelConsumptionInput != null) {
                        // Use up to 2 decimal places
                        fuelConsumptionInput.setText(String.format(java.util.Locale.US, "%.2f", body.fuelLiters));
                    }

                    // Auto-fill description if detected (e.g., XCS, Xtra Advance)
                    if (body.description != null && descriptionInput != null) {
                        if (descriptionContainer != null) {
                            descriptionContainer.setVisibility(View.VISIBLE);
                        }
                        descriptionInput.setText(body.description);
                    }

                    // Auto-fill serial number if detected
                    if (body.serialNumber != null && serialNumberInput != null) {
                        if (serialNumberContainer != null) {
                            serialNumberContainer.setVisibility(View.VISIBLE);
                        }
                        serialNumberInput.setText(body.serialNumber);
                    }

                    // Auto-fill invoice date if detected
                    if (body.invoiceDate != null && invoiceDateInput != null) {
                        if (invoiceDateContainer != null) {
                            invoiceDateContainer.setVisibility(View.VISIBLE);
                        }
                        invoiceDateInput.setText(body.invoiceDate);
                    }

                    // Re-evaluate form completeness so "Add Expenses" / "Mark as done" can be enabled
                    updateAddButtonEnabled();
                }

                @Override
                public void onFailure(Call<ReceiptAnalysisResponse> call, Throwable t) {
                    Toast.makeText(context, "Could not analyze receipt", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, "Error preparing receipt for analysis", Toast.LENGTH_SHORT).show();
        }
    }

    // Show a combined date + time picker for invoice date
    private void showInvoiceDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                (Activity) context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePicker = new TimePickerDialog(
                            (Activity) context,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US);
                                String formatted = sdf.format(calendar.getTime());
                                if (invoiceDateInput != null) {
                                    invoiceDateInput.setText(formatted);
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.show();
    }

}
