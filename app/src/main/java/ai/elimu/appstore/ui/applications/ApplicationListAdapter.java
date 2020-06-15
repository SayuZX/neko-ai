package ai.elimu.appstore.ui.applications;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import ai.elimu.appstore.BaseApplication;
import ai.elimu.appstore.BuildConfig;
import ai.elimu.appstore.R;
import ai.elimu.appstore.room.entity.Application;
import ai.elimu.appstore.room.entity.ApplicationVersion;
import ai.elimu.appstore.util.FileHelper;
import ai.elimu.appstore.util.InstallationHelper;
import ai.elimu.appstore.util.SharedPreferencesHelper;
import ai.elimu.model.enums.admin.ApplicationStatus;
import timber.log.Timber;

public class ApplicationListAdapter extends RecyclerView.Adapter<ApplicationListAdapter.ApplicationViewHolder> {

    private final LayoutInflater layoutInflater;

    private final Context context;

    private List<Application> applications;

    private List<ApplicationVersion> applicationVersions;

    public ApplicationListAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public ApplicationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.i("onCreateViewHolder");
        View itemView = layoutInflater.inflate(R.layout.activity_application_list_item, parent, false);
        return new ApplicationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ApplicationViewHolder viewHolder, int position) {
        Timber.i("onBindViewHolder");
        if (applications != null) {
            Application application = applications.get(position);
            viewHolder.textViewFirstLine.setText(application.getPackageName());
            viewHolder.textViewSecondLine.setText(
//                    application.getApplicationStatus().toString() + ", " +
//                    application.getLiteracySkills() + ", " +
//                    application.getNumeracySkills()
                    application.getApplicationStatus().toString()
            );

            if (application.getApplicationStatus() != ApplicationStatus.ACTIVE) {
                viewHolder.textViewFirstLine.setAlpha(0.5f);
                viewHolder.textViewSecondLine.setAlpha(0.5f);
            }

            // If the APK has been installed, display the "Launch" button
//            if (InstallationHelper.isApplicationInstalled(application.getPackageName(), context)) {
//                viewHolder.launchButton.setVisibility(View.VISIBLE);
//                viewHolder.launchButton.setOnClickListener((View.OnClickListener) v -> {
//                    Timber.i("onClick");
//
//                    Timber.i("Launching \"" + application.getPackageName() + "\"");
//                    PackageManager packageManager = context.getPackageManager();
//                    Intent launchIntent = packageManager.getLaunchIntentForPackage(application.getPackageName());
//                    Timber.i("launchIntent: " + launchIntent);
//                    context.startActivity(launchIntent);
//                });
//            } else {
                // Fetch information about the newest APK file
                ApplicationVersion applicationVersion = getNewestApplicationVersion(application, applicationVersions);
                if (applicationVersion == null) {
                    return;
                }
                Timber.i("applicationVersion.getVersionCode(): " + applicationVersion.getVersionCode());

                // If the APK has been downloaded (but not yet installed), display the "Install" button
                File apkFile = FileHelper.getApkFile(application.getPackageName(), applicationVersion.getVersionCode(), context);
                Timber.i("apkFile: " + apkFile);
                Timber.i("apkFile.exists(): " + apkFile.exists());
                if (apkFile.exists()) {
                    viewHolder.installButton.setVisibility(View.VISIBLE);
                    viewHolder.installButton.setOnClickListener(v -> {
                        Timber.i("viewHolder.installButton onClick");

                        // Initiate installation of the APK file
                        Uri apkUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".apk.provider", apkFile);
                        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent.setData(apkUri);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(intent);
                    });
                }

                // If the APK has not been downloaded, display the "Download" button
                if (!apkFile.exists()) {
                    viewHolder.downloadButton.setVisibility(View.VISIBLE);
                    ApplicationVersion finalApplicationVersion = applicationVersion;
                    viewHolder.downloadButton.setOnClickListener(v -> {
                        Timber.i("viewHolder.downloadButton onClick");

                        // Initiate download of the APK file
                        BaseApplication baseApplication = (BaseApplication) context.getApplicationContext();
                        String fileUrl = baseApplication.getBaseUrl() + finalApplicationVersion.getFileUrl();
                        Timber.i("fileUrl: " +  fileUrl);
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
                        String destinationInExternalFilesDir = File.separator + "lang-" + SharedPreferencesHelper.getLanguage(context).getIsoCode() + File.separator + "apks" + File.separator + apkFile.getName();
                        Timber.i("destinationInExternalFilesDir: " +  destinationInExternalFilesDir);
                        request.setDestinationInExternalFilesDir(context, null, destinationInExternalFilesDir);
                        long downloadId = downloadManager.enqueue(request);
                        Timber.i("downloadId: " +  downloadId);

                        // Replace download button with progress bar
                        viewHolder.downloadButton.setVisibility(View.INVISIBLE);
                        viewHolder.downloadProgressBar.setVisibility(View.VISIBLE);
                    });
                }
//            }
        }
    }

    private ApplicationVersion getNewestApplicationVersion(Application application, List<ApplicationVersion> applicationVersions) {
        ApplicationVersion applicationVersion = null;
        for (ApplicationVersion appVersion : applicationVersions) {
            if (appVersion.getApplicationId() == application.getId()) {
                applicationVersion = appVersion;
                break;
            }
        }
        return applicationVersion;
    }

    @Override
    public int getItemCount() {
        Timber.i("getItemCount");
        if (applications == null) {
            return 0;
        } else {
            return applications.size();
        }
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public void setApplicationVersions(List<ApplicationVersion> applicationVersions) {
        this.applicationVersions = applicationVersions;
    }


    class ApplicationViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewFirstLine;
        private final TextView textViewSecondLine;

        private Button launchButton;
        private Button installButton;
        private Button downloadButton;
        private ProgressBar downloadProgressBar;

        private ApplicationViewHolder(View itemView) {
            super(itemView);
            Timber.i("ApplicationViewHolder");

            textViewFirstLine = itemView.findViewById(R.id.textViewFirstLine);
            textViewSecondLine = itemView.findViewById(R.id.textViewSecondLine);

            launchButton = itemView.findViewById(R.id.list_item_launch_button);
            installButton = itemView.findViewById(R.id.list_item_install_button);
            downloadButton = itemView.findViewById(R.id.list_item_download_button);
            downloadProgressBar = itemView.findViewById(R.id.list_item_download_progressbar);
        }
    }
}
