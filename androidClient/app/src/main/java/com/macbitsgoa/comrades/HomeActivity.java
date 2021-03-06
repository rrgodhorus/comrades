package com.macbitsgoa.comrades;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.macbitsgoa.comrades.aboutmac.AboutMacActivity;
import com.macbitsgoa.comrades.courselistfragment.CourseListFragment;
import com.macbitsgoa.comrades.homefragment.HomeFragment;
import com.macbitsgoa.comrades.persistance.Database;
import com.macbitsgoa.comrades.profilefragment.ProfileFragment;
import com.macbitsgoa.comrades.search.SearchCoursesCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class HomeActivity extends AppCompatActivity {
    public static String SETTINGS = "NotificationSetting";
    private GoogleApiClient mGoogleApiClient;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    public static BottomNavigationView navigation;
    private SearchView searchView;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                fragmentManager.beginTransaction().replace(R.id.container_fragment,
                        HomeFragment.newInstance()).commit();
                return true;
            case R.id.navigation_courses:
                fragmentManager.beginTransaction().replace(R.id.container_fragment,
                        CourseListFragment.newInstance()).commit();
                return true;
            case R.id.navigation_profile:
                fragmentManager.beginTransaction().replace(R.id.container_fragment,
                        ProfileFragment.newInstance()).commit();
                return true;
        }
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_home);
        setSupportActionBar(findViewById(R.id.toolbar_main_act));
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (savedInstanceState != null) {
            navigation.setSelectedItemId(savedInstanceState.getInt("bottomNav"));
        } else {
            navigation.setSelectedItemId(R.id.navigation_home);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_toolbar, menu);
        final boolean signedIn = GoogleSignIn.getLastSignedInAccount(this) != null;
        final MenuItem signOut = menu.findItem(R.id.action_sign_out);
        signOut.setVisible(signedIn);
        signOut.setOnMenuItemClickListener(menuItem -> {
            FirebaseAuth.getInstance().signOut();
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(status -> {
                invalidateOptionsMenu();
                Toast.makeText(HomeActivity.this, "Signed Out Successfully", Toast.LENGTH_SHORT).show();
                navigation.setSelectedItemId(navigation.getSelectedItemId());
            });
            return true;
        });

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.e("TAG", "query:" + s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.e("TAG", "query:" + s);
                getCoursesFromDb(s);
                return false;
            }
        });

        final MenuItem aboutMac = menu.findItem(R.id.action_about_mac);
        aboutMac.setOnMenuItemClickListener(menuItem1 -> {
            startActivity(new Intent(this, AboutMacActivity.class));
            return true;

        });
        return true;

    }

    @Override
    protected void onStart() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Boolean previousStarted = preferences.getBoolean("Previously Started", false);
        if (!previousStarted) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.putBoolean(SETTINGS, true);
            edit.putBoolean("Previously Started", Boolean.TRUE);
            edit.apply();
        }
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("bottomNav", navigation.getSelectedItemId());
    }

    private void getCoursesFromDb(String query) {
        String searchText = "%" + query + "%";
        Observable.just(searchText).observeOn(Schedulers.computation())
                .map(new Function<String, Cursor>() {
                    @Override
                    public Cursor apply(String searchStrt) {
                        return Database.getInstance(HomeActivity.this).getCourseDao().getSearchCursor(searchStrt);
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Cursor>() {
                    @Override
                    public void accept(Cursor cursor) {
                        handleResults(cursor);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        handleError(throwable);
                    }
                });

    }


    private void handleResults(Cursor cursor) {
        searchView.setSuggestionsAdapter(new SearchCoursesCursorAdapter(HomeActivity.this, cursor));
    }

    private void handleError(Throwable t) {
        Log.e("TAG", t.getMessage(), t);
        Toast.makeText(this, "Problem in Fetching Courses",
                Toast.LENGTH_LONG).show();
    }

}