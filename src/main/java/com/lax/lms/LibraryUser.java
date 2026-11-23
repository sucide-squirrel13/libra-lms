package com.lax.lms;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "library_users")
public class LibraryUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @Column(nullable = false)
    private String role = "USER";

    @Column(name = "theme_preference")
    private String themePreference = "dark";

    @Column(name = "accent_color")
    private String accentColor = "gold";

    @Column(name = "interface_density")
    private String interfaceDensity = "comfortable";

    @Column(name = "font_size_preference")
    private String fontSizePreference = "normal";

    @Column(name = "content_width")
    private String contentWidth = "standard";

    @Column(name = "reading_layout")
    private String readingLayout = "grid";

    @Column(name = "email_borrow_reminders")
    private Boolean emailBorrowReminders = true;

    @Column(name = "email_due_reminders")
    private Boolean emailDueReminders = true;

    @Column(name = "email_return_confirmations")
    private Boolean emailReturnConfirmations = true;

    @Column(name = "reduced_motion_preference")
    private Boolean reducedMotionPreference = false;

    @Column(name = "high_contrast_preference")
    private Boolean highContrastPreference = false;

    @Column(name = "default_landing_page")
    private String defaultLandingPage = "dashboard";

    @Column(name = "date_format_preference")
    private String dateFormatPreference = "YYYY-MM-DD";

    @Column(name = "timezone_preference")
    private String timezonePreference = "UTC";

    public LibraryUser() {
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference != null ? themePreference : "dark";
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor != null ? accentColor : "gold";
    }

    public String getInterfaceDensity() {
        return interfaceDensity;
    }

    public void setInterfaceDensity(String interfaceDensity) {
        this.interfaceDensity = interfaceDensity != null ? interfaceDensity : "comfortable";
    }

    public String getFontSizePreference() {
        return fontSizePreference;
    }

    public void setFontSizePreference(String fontSizePreference) {
        this.fontSizePreference = fontSizePreference != null ? fontSizePreference : "normal";
    }

    public String getContentWidth() {
        return contentWidth;
    }

    public void setContentWidth(String contentWidth) {
        this.contentWidth = contentWidth != null ? contentWidth : "standard";
    }

    public String getReadingLayout() {
        return readingLayout;
    }

    public void setReadingLayout(String readingLayout) {
        this.readingLayout = readingLayout != null ? readingLayout : "grid";
    }

    public boolean isEmailBorrowReminders() {
        return emailBorrowReminders != null && emailBorrowReminders;
    }

    public void setEmailBorrowReminders(Boolean emailBorrowReminders) {
        this.emailBorrowReminders = emailBorrowReminders != null ? emailBorrowReminders : true;
    }

    public boolean isEmailDueReminders() {
        return emailDueReminders != null && emailDueReminders;
    }

    public void setEmailDueReminders(Boolean emailDueReminders) {
        this.emailDueReminders = emailDueReminders != null ? emailDueReminders : true;
    }

    public boolean isEmailReturnConfirmations() {
        return emailReturnConfirmations != null && emailReturnConfirmations;
    }

    public void setEmailReturnConfirmations(Boolean emailReturnConfirmations) {
        this.emailReturnConfirmations = emailReturnConfirmations != null ? emailReturnConfirmations : true;
    }

    public boolean isReducedMotionPreference() {
        return reducedMotionPreference != null && reducedMotionPreference;
    }

    public void setReducedMotionPreference(Boolean reducedMotionPreference) {
        this.reducedMotionPreference = reducedMotionPreference != null ? reducedMotionPreference : false;
    }

    public boolean isHighContrastPreference() {
        return highContrastPreference != null && highContrastPreference;
    }

    public void setHighContrastPreference(Boolean highContrastPreference) {
        this.highContrastPreference = highContrastPreference != null ? highContrastPreference : false;
    }

    public String getDefaultLandingPage() {
        return defaultLandingPage;
    }

    public void setDefaultLandingPage(String defaultLandingPage) {
        this.defaultLandingPage = defaultLandingPage != null ? defaultLandingPage : "dashboard";
    }

    public String getDateFormatPreference() {
        return dateFormatPreference;
    }

    public void setDateFormatPreference(String dateFormatPreference) {
        this.dateFormatPreference = dateFormatPreference != null ? dateFormatPreference : "YYYY-MM-DD";
    }

    public String getTimezonePreference() {
        return timezonePreference;
    }

    public void setTimezonePreference(String timezonePreference) {
        this.timezonePreference = timezonePreference != null ? timezonePreference : "UTC";
    }
}