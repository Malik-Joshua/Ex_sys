package ExhibitionRegistrationSystem.java; // Default package name. CHANGE THIS if your NetBeans project has a different package.

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import javax.imageio.ImageIO;

public class ExhibitionRegistrationSystem extends JFrame {

    // --- GUI Components ---
    private JTextField regIdField, nameField, facultyField, projectTitleField, contactField, emailField;
    private JButton registerButton, searchButton, updateButton, deleteButton, clearButton, exitButton, uploadButton;
    private JLabel imageLabel;
    private JPanel mainPanel, formPanel, imagePanel, buttonPanel;

    // --- Database & Image Handling ---
    private Connection dbConnection;
    private byte[] projectImageBytes; // To hold the image data

    // --- Database Connection String ---
    // Assumes the DB file is in the root directory of the NetBeans project
    private static final String DATABASE_URL = "jdbc:ucanaccess://VUE_Exhibition.accdb";

    public ExhibitionRegistrationSystem() {
        // --- Frame Setup ---
        setTitle("Victoria University - Innovation Exhibition Registration");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // --- Main Panel ---
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // --- Initialize Components ---
        initComponents();
        
        // --- Layout Panels ---
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(imagePanel, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // --- Connect to Database on Startup ---
        connectToDatabase();

        // --- Add Action Listeners for Buttons ---
        addActionListeners();
    }

    /**
     * Initializes and lays out all GUI components.
     */
    private void initComponents() {
        // --- Form Panel (using GridBagLayout for flexible form design) ---
        formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.anchor = GridBagConstraints.WEST;

        // Labels
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Registration ID:"), gbc);
        gbc.gridy++; formPanel.add(new JLabel("Student Name:"), gbc);
        gbc.gridy++; formPanel.add(new JLabel("Faculty:"), gbc);
        gbc.gridy++; formPanel.add(new JLabel("Project Title:"), gbc);
        gbc.gridy++; formPanel.add(new JLabel("Contact Number:"), gbc);
        gbc.gridy++; formPanel.add(new JLabel("Email Address:"), gbc);

        // Text Fields
        regIdField = new JTextField(20);
        regIdField.setToolTipText("Enter ID and click Search. ID is auto-generated for new entries.");
        nameField = new JTextField(20);
        facultyField = new JTextField(20);
        projectTitleField = new JTextField(20);
        contactField = new JTextField(20);
        emailField = new JTextField(20);

        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(regIdField, gbc);
        gbc.gridy++; formPanel.add(nameField, gbc);
        gbc.gridy++; formPanel.add(facultyField, gbc);
        gbc.gridy++; formPanel.add(projectTitleField, gbc);
        gbc.gridy++; formPanel.add(contactField, gbc);
        gbc.gridy++; formPanel.add(emailField, gbc);

        // --- Image Panel ---
        imagePanel = new JPanel(new BorderLayout(5, 5));
        imagePanel.setPreferredSize(new Dimension(250, 250));
        imageLabel = new JLabel("Project Prototype Image", SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.WHITE);
        uploadButton = new JButton("Upload Image");
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        imagePanel.add(uploadButton, BorderLayout.SOUTH);
        
        // --- Button Panel ---
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        registerButton = new JButton("Register");
        searchButton = new JButton("Search");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        clearButton = new JButton("Clear");
        exitButton = new JButton("Exit");
        
        buttonPanel.add(registerButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(exitButton);
    }
    
    /**
     * Registers all action listeners for the buttons.
     */
    private void addActionListeners() {
        registerButton.addActionListener(e -> registerParticipant());
        searchButton.addActionListener(e -> searchParticipant());
        updateButton.addActionListener(e -> updateParticipant());
        deleteButton.addActionListener(e -> deleteParticipant());
        uploadButton.addActionListener(e -> uploadImage());
        clearButton.addActionListener(e -> clearForm());

        exitButton.addActionListener(e -> {
            try {
                if (dbConnection != null && !dbConnection.isClosed()) {
                    dbConnection.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error closing database connection: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(0);
        });
    }

    /**
     * Establishes a connection to the MS Access database.
     */
    private void connectToDatabase() {
        try {
            dbConnection = DriverManager.getConnection(DATABASE_URL);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to connect to the database.\n" +
                "Error: " + e.getMessage() + "\n" +
                "Please ensure 'VUE_Exhibition.accdb' exists in the project's root directory.",
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Handles the participant registration logic.
     */
    private void registerParticipant() {
        if (nameField.getText().trim().isEmpty() || projectTitleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Student Name and Project Title are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO Participants (StudentName, Faculty, ProjectTitle, ContactNumber, EmailAddress, ProjectImage) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, nameField.getText());
            pstmt.setString(2, facultyField.getText());
            pstmt.setString(3, projectTitleField.getText());
            pstmt.setString(4, contactField.getText());
            pstmt.setString(5, emailField.getText());
            
            if (projectImageBytes != null) {
                pstmt.setBytes(6, projectImageBytes);
            } else {
                pstmt.setNull(6, Types.BINARY);
            }
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newId = generatedKeys.getInt(1);
                        JOptionPane.showMessageDialog(this, "Participant registered successfully with ID: " + newId, "Success", JOptionPane.INFORMATION_MESSAGE);
                        clearForm();
                    }
                }
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error registering participant: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Searches for a participant by Registration ID and populates the form.
     */
    private void searchParticipant() {
        String idText = regIdField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Registration ID to search.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT * FROM Participants WHERE RegistrationID = ?";
        
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            int regId = Integer.parseInt(idText);
            pstmt.setInt(1, regId);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                nameField.setText(rs.getString("StudentName"));
                facultyField.setText(rs.getString("Faculty"));
                projectTitleField.setText(rs.getString("ProjectTitle"));
                contactField.setText(rs.getString("ContactNumber"));
                emailField.setText(rs.getString("EmailAddress"));
                
                projectImageBytes = rs.getBytes("ProjectImage");
                if (projectImageBytes != null && projectImageBytes.length > 0) {
                    displayImage(projectImageBytes);
                } else {
                    imageLabel.setIcon(null);
                    imageLabel.setText("No Image Available");
                }
            } else {
                JOptionPane.showMessageDialog(this, "No participant found with ID: " + regId, "Search Result", JOptionPane.INFORMATION_MESSAGE);
                clearForm(false);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error during search: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Registration ID. Please enter a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Updates an existing participant's record.
     */
    private void updateParticipant() {
        String idText = regIdField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please search for a participant first to get their ID.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "UPDATE Participants SET StudentName = ?, Faculty = ?, ProjectTitle = ?, ContactNumber = ?, EmailAddress = ?, ProjectImage = ? WHERE RegistrationID = ?";
        
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, nameField.getText());
            pstmt.setString(2, facultyField.getText());
            pstmt.setString(3, projectTitleField.getText());
            pstmt.setString(4, contactField.getText());
            pstmt.setString(5, emailField.getText());
            
            if (projectImageBytes != null) {
                pstmt.setBytes(6, projectImageBytes);
            } else {
                pstmt.setNull(6, Types.BINARY);
            }
            
            pstmt.setInt(7, Integer.parseInt(idText));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Participant record updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                 JOptionPane.showMessageDialog(this, "Update failed. No participant found with the specified ID.", "Update Failed", JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating record: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Registration ID.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Deletes a participant's record from the database.
     */
    private void deleteParticipant() {
        String idText = regIdField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please search for a participant to delete.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this participant's record?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM Participants WHERE RegistrationID = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(idText));
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Record deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Deletion failed. No record found with that ID.", "Deletion Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting record: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
             JOptionPane.showMessageDialog(this, "Invalid Registration ID.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * Opens a file chooser to select an image and loads it into memory.
     */
    private void uploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Project Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "gif", "bmp"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                projectImageBytes = Files.readAllBytes(selectedFile.toPath());
                displayImage(projectImageBytes);
                JOptionPane.showMessageDialog(this, "Image uploaded successfully. Click Register or Update to save.", "Image Upload", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading image file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                projectImageBytes = null;
            }
        }
    }

    /**
     * Resizes and displays the given image bytes in the imageLabel.
     * @param imageData The byte array of the image.
     */
    private void displayImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) return;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage bImage = ImageIO.read(bis);
            if (bImage != null) {
                Image scaledImage = bImage.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                imageLabel.setText("");
            }
        } catch (IOException e) {
            imageLabel.setIcon(null);
            imageLabel.setText("Image display error");
        }
    }

    /**
     * Clears all fields in the form.
     */
    private void clearForm() {
        clearForm(true);
    }
    
    /**
     * Overloaded clearForm method.
     * @param clearIdField If true, clears the Registration ID field.
     */
    private void clearForm(boolean clearIdField) {
        if (clearIdField) {
            regIdField.setText("");
        }
        nameField.setText("");
        facultyField.setText("");
        projectTitleField.setText("");
        contactField.setText("");
        emailField.setText("");
        imageLabel.setIcon(null);
        imageLabel.setText("Project Prototype Image");
        projectImageBytes = null;
    }

    /**
     * The main entry point for the application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExhibitionRegistrationSystem frame = new ExhibitionRegistrationSystem();
            frame.setVisible(true);
        });
    }
}

