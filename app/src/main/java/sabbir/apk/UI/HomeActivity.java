package sabbir.apk.UI;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.R;
import sabbir.apk.Reminder.ReminderScheduler;
import sabbir.apk.UI.home.HomeScheduleController;
import sabbir.apk.UI.home.HomeUpdateController;

public final class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private HomeScheduleController scheduleController;
    private HomeUpdateController updateController;

    private MaterialCardView cardCurrentClass;
    private RecyclerView rvUpcoming;
    private RecyclerView rvPrevious;
    private android.widget.TextView tvCurrentSubject;
    private android.widget.TextView tvCurrentInstructor;
    private android.widget.TextView tvCurrentTime;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupToolbarAndDrawer();
        setupRecyclerViews();
        setupControllers();
        loadRoutine();
        updateController.checkForAppUpdate();
    }

    private void setupControllers() {
        scheduleController = new HomeScheduleController(
                cardCurrentClass,
                rvUpcoming,
                rvPrevious,
                tvCurrentSubject,
                tvCurrentInstructor,
                tvCurrentTime,
                findViewById(R.id.tv_upcoming_header),
                findViewById(R.id.tv_previous_header),
                (items, date) -> ReminderScheduler.scheduleTodayReminders(this, items, date)
        );

        updateController = new HomeUpdateController(this, requestCode -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startActivityForResult(
                        new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                .setData(android.net.Uri.parse("package:" + getPackageName())),
                        requestCode
                );
            }
        });
    }

    private void loadRoutine() {
        File routineFile = RoutineManagerApi.getRoutineFile(this);
        if (routineFile == null || !routineFile.exists() || !routineFile.canRead()) {
            scheduleController.showErrorState("Routine file not found or inaccessible");
            return;
        }
        loadRoutineAsync(routineFile);
    }

    private void initViews() {
        cardCurrentClass = findViewById(R.id.card_current_class);
        rvUpcoming = findViewById(R.id.rv_upcoming);
        rvPrevious = findViewById(R.id.rv_previous);
        tvCurrentSubject = findViewById(R.id.tv_current_subject);
        tvCurrentInstructor = findViewById(R.id.tv_current_instructor);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);

        androidx.appcompat.app.ActionBarDrawerToggle toggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START, true);
            handleNavigationItemClick(item.getItemId());
            return true;
        });
    }

    private void setupRecyclerViews() {
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPrevious.setLayoutManager(new LinearLayoutManager(this));
        rvUpcoming.setNestedScrollingEnabled(false);
        rvPrevious.setNestedScrollingEnabled(false);
    }

    private void loadRoutineAsync(@NonNull File file) {
        ioExecutor.execute(() -> {
            try {
                String json = readFileToString(file);
                JSONObject root = new JSONObject(json);
                mainHandler.post(() -> renderSchedule(root));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load routine", e);
                mainHandler.post(() -> scheduleController.showErrorState("Failed to load routine"));
            }
        });
    }

    private void renderSchedule(JSONObject root) {
        try {
            scheduleController.renderTodaySchedule(root);
        } catch (Exception e) {
            Log.e(TAG, "Invalid routine JSON structure", e);
            scheduleController.showErrorState("Invalid schedule format");
        }
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void handleNavigationItemClick(int itemId) {
        if (itemId == R.id.menu_today) {
            return;
        }

        Class<?> destination = itemId == R.id.menu_schedule ? Schedule.class : Setting.class;
        startActivity(new Intent(this, destination)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HomeUpdateController.REQUEST_INSTALL_PACKAGES_CODE) {
            updateController.onInstallPermissionResult();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (scheduleController != null && scheduleController.hasSchedule()) {
            scheduleController.startUiTicker();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scheduleController != null) {
            scheduleController.stopUiTicker();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateController != null) {
            updateController.onDestroy();
        }
        if (scheduleController != null) {
            scheduleController.stopUiTicker();
        }
        ioExecutor.shutdownNow();
    }
}
