package xyz.aungpyaephyo.padc.myanmarattractions.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import xyz.aungpyaephyo.padc.myanmarattractions.R;
import xyz.aungpyaephyo.padc.myanmarattractions.adapters.CountryListAdapter;
import xyz.aungpyaephyo.padc.myanmarattractions.controllers.UserSessionController;
import xyz.aungpyaephyo.padc.myanmarattractions.data.vos.UserVO;
import xyz.aungpyaephyo.padc.myanmarattractions.dialogs.SharedDialog;
import xyz.aungpyaephyo.padc.myanmarattractions.events.DataEvent;
import xyz.aungpyaephyo.padc.myanmarattractions.utils.CommonUtils;
import xyz.aungpyaephyo.padc.myanmarattractions.utils.FacebookUtils;
import xyz.aungpyaephyo.padc.myanmarattractions.views.PasswordVisibilityListener;

/**
 * Created by aung on 7/15/16.
 */
public class RegisterFragment extends BaseFragment {

    @BindView(R.id.lbl_registration_title)
    TextView lblRegistrationTitle;

    @BindView(R.id.et_name)
    EditText etName;

    @BindView(R.id.et_email)
    EditText etEmail;

    @BindView(R.id.et_password)
    EditText etPassword;

    @BindView(R.id.tv_date_of_birth)
    TextView tvDateOfBirth;

    @BindView(R.id.sp_country_list)
    Spinner spCountryList;

    private CountryListAdapter mCountryListAdapter;
    private UserSessionController mUserSessionController;

    private UserVO mRegisteringUser;

    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUserSessionController = (UserSessionController) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] countryListArray = getResources().getStringArray(R.array.current_residing_country);
        List<String> countryList = new ArrayList<>(Arrays.asList(countryListArray));

        mCountryListAdapter = new CountryListAdapter(countryList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_register, container, false);
        ButterKnife.bind(this, rootView);

        lblRegistrationTitle.setText(Html.fromHtml(getString(R.string.lbl_registration_title)));
        spCountryList.setAdapter(mCountryListAdapter);
        etPassword.setOnTouchListener(new PasswordVisibilityListener());

        tvDateOfBirth.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showDatePicker();
                }
            }
        });

        tvDateOfBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus eventBus = EventBus.getDefault();
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus eventBus = EventBus.getDefault();
        eventBus.unregister(this);
    }

    private void showDatePicker() {
        DialogFragment newFragment = new DatePickerDialogFragment();
        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    @OnClick(R.id.btn_register)
    public void onTapRegister(Button btnRegister) {
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        String dateOfBith = tvDateOfBirth.getText().toString();
        String country = String.valueOf(spCountryList.getSelectedItem());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(dateOfBith)) {
            //One of the required data is empty
            if (TextUtils.isEmpty(name)) {
                etName.setError(getString(R.string.error_missing_name));
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError(getString(R.string.error_missing_email));
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError(getString(R.string.error_missing_password));
            }

            if (TextUtils.isEmpty(dateOfBith)) {
                tvDateOfBirth.setError(getString(R.string.error_missing_date_of_birth));
            }
        } else if (!CommonUtils.isEmailValid(email)) {
            //Email address is not in the right format.
            etEmail.setError(getString(R.string.error_email_is_not_valid));
        } else {
            //Checking on client side is done here.
            if(mRegisteringUser == null) { //Regular Registration
                mUserSessionController.onRegister(name, email, password, dateOfBith, country);
            } else { //Registration with Social Media.
                mRegisteringUser.setDateOfBirthText(dateOfBith);
                mRegisteringUser.setCountryOfOrigin(country);

                mUserSessionController.onRegisterWithFacebook(mRegisteringUser, password);
            }

        }

    }

    @OnClick(R.id.iv_register_with_facebook)
    public void onTapRegisterWithFacebook(View view) {
        if (AccessToken.getCurrentAccessToken() == null) {
            //Haven't login
            Toast.makeText(getContext(), "Logging In ...", Toast.LENGTH_SHORT).show();
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(FacebookUtils.FACEBOOK_LOGIN_PERMISSIONS));
        } else {
            //Logout - just to test it.
            Toast.makeText(getContext(), "Logging Out ...", Toast.LENGTH_SHORT).show();
            LoginManager.getInstance().logOut();
        }
    }

    @Override
    protected void onRetrieveFacebookInfo(JSONObject facebookLoginUser, String imageUrl, String coverImageUrl) {
        super.onRetrieveFacebookInfo(facebookLoginUser, imageUrl, coverImageUrl);
        mRegisteringUser = UserVO.initFromFacebookInfo(facebookLoginUser, imageUrl, coverImageUrl);

        showRetrievedDataInRegistrationForm(mRegisteringUser);
    }

    private void showRetrievedDataInRegistrationForm(UserVO registeringUser) {
        if (!TextUtils.isEmpty(registeringUser.getName())) {
            etName.setText(registeringUser.getName());
        }

        if (!TextUtils.isEmpty(registeringUser.getEmail())) {
            etEmail.setText(registeringUser.getEmail());
        }

        etPassword.requestFocus();

        SharedDialog.promptMsgWithTheme(getActivity(),
                getString(R.string.prompt_some_data_retrieved_for_registration));
    }

    //Success Register
    public void onEventMainThread(DataEvent.DatePickedEvent event) {
        tvDateOfBirth.setText(event.getDateOfBrith());
    }
}
