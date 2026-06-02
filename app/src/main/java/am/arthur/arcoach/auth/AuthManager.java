package am.arthur.arcoach.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import am.arthur.arcoach.utils.MyLog;

public class AuthManager {

    private static final String TAG = "AuthManager";
    private final FirebaseAuth firebaseAuth;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }

    public interface SignOutCallback {
        void onSignedOut();
    }

    public AuthManager(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        MyLog.d(TAG, "AuthManager initialized");
    }

    /**
     * Проверить залогинен ли пользователь
     */
    public boolean isLoggedIn() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        boolean loggedIn = user != null;
        MyLog.d(TAG, "User logged in: " + loggedIn);
        return loggedIn;
    }

    /**
     * Получить текущего пользователя
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Получить email текущего пользователя
     */
    public String getUserEmail() {
        FirebaseUser user = getCurrentUser();
        if (user != null && user.getEmail() != null) {
            return user.getEmail();
        }
        return "Not signed in";
    }

    /**
     * Получить display name пользователя
     */
    public String getUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String email = user.getEmail();
            return email.substring(0, email.indexOf("@"));
        }
        return "User";
    }

    /**
     * Получить UID пользователя
     */
    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return null;
    }

    /**
     * Регистрация нового пользователя
     */
    public void signUp(String email, String password, AuthCallback callback) {
        MyLog.d(TAG, "Creating new account: " + email);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            MyLog.d(TAG, "Sign up successful: " + email);
                            callback.onSuccess(user);
                        } else {
                            MyLog.e(TAG, "Sign up failed", task.getException());
                            String error = getErrorMessage(task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Вход существующего пользователя
     */
    public void signIn(String email, String password, AuthCallback callback) {
        MyLog.d(TAG, "Signing in: " + email);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            MyLog.d(TAG, "Sign in successful: " + email);
                            callback.onSuccess(user);
                        } else {
                            MyLog.e(TAG, "Sign in failed", task.getException());
                            String error = getErrorMessage(task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Выход из аккаунта
     */
    public void signOut(SignOutCallback callback) {
        MyLog.d(TAG, "Signing out user");
        firebaseAuth.signOut();

        if (callback != null) {
            callback.onSignedOut();
        }
    }

    /**
     * Сброс пароля (отправка email)
     */
    public void resetPassword(String email, AuthCallback callback) {
        MyLog.d(TAG, "Sending password reset email to: " + email);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            MyLog.d(TAG, "Password reset email sent");
                            callback.onSuccess(null);
                        } else {
                            MyLog.e(TAG, "Failed to send password reset email", task.getException());
                            String error = getErrorMessage(task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Изменить пароль (требует повторную аутентификацию)
     */
    public void changePassword(String currentPassword, String newPassword, AuthCallback callback) {
        FirebaseUser user = getCurrentUser();

        if (user == null || user.getEmail() == null) {
            callback.onFailure("User not logged in");
            return;
        }

        MyLog.d(TAG, "Changing password for: " + user.getEmail());

        // Firebase требует повторную аутентификацию перед изменением пароля
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(
                        user.getEmail(),
                        currentPassword
                );

        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            MyLog.d(TAG, "Re-authentication successful, updating password");

                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> updateTask) {
                                            if (updateTask.isSuccessful()) {
                                                MyLog.d(TAG, "Password changed successfully");
                                                callback.onSuccess(user);
                                            } else {
                                                MyLog.e(TAG, "Failed to update password", updateTask.getException());
                                                String error = getErrorMessage(updateTask.getException());
                                                callback.onFailure(error);
                                            }
                                        }
                                    });
                        } else {
                            MyLog.e(TAG, "Re-authentication failed - wrong current password", task.getException());
                            callback.onFailure("Current password is incorrect");
                        }
                    }
                });
    }

    /**
     * Получить понятное сообщение об ошибке
     */
    private String getErrorMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error";
        }

        String message = exception.getMessage();

        if (message == null) {
            return "Authentication failed";
        }

        if (message.contains("email address is already in use")) {
            return "This email is already registered";
        } else if (message.contains("email address is badly formatted")) {
            return "Invalid email address";
        } else if (message.contains("password is invalid") || message.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "Wrong email or password";
        } else if (message.contains("no user record")) {
            return "Account not found. Please sign up first.";
        } else if (message.contains("network error")) {
            return "Network error. Check your connection.";
        } else if (message.contains("weak password")) {
            return "Password must be at least 6 characters";
        }

        return "Authentication failed. Please try again.";
    }

    /**
     * Добавить слушатель изменения состояния авторизации
     */
    public void addAuthStateListener(FirebaseAuth.AuthStateListener listener) {
        firebaseAuth.addAuthStateListener(listener);
    }

    /**
     * Удалить слушатель изменения состояния авторизации
     */
    public void removeAuthStateListener(FirebaseAuth.AuthStateListener listener) {
        firebaseAuth.removeAuthStateListener(listener);
    }

    /**
     * Отправить email для верификации
     */
    public void sendEmailVerification(AuthCallback callback) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onFailure("User not logged in");
            return;
        }

        if (user.isEmailVerified()) {
            MyLog.d(TAG, "Email already verified");
            callback.onSuccess(user);
            return;
        }

        MyLog.d(TAG, "Sending verification email to: " + user.getEmail());

        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            MyLog.d(TAG, "Verification email sent successfully");
                            callback.onSuccess(user);
                        } else {
                            MyLog.e(TAG, "Failed to send verification email", task.getException());
                            String error = getErrorMessage(task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Проверить верифицирован ли email
     */
    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            user.reload();
            return user.isEmailVerified();
        }
        return false;
    }

    /**
     * Обновить статус верификации (reload user data)
     */
    public void refreshEmailVerificationStatus(AuthCallback callback) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onFailure("User not logged in");
            return;
        }

        MyLog.d(TAG, "Refreshing email verification status");

        user.reload()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            boolean isVerified = user.isEmailVerified();
                            MyLog.d(TAG, "Email verified: " + isVerified);
                            callback.onSuccess(user);
                        } else {
                            MyLog.e(TAG, "Failed to refresh user data", task.getException());
                            callback.onFailure("Failed to refresh status");
                        }
                    }
                });
    }
}
