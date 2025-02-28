//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.broker.flw;

import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.ErrorCodes;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// End My Shift - In Shared device mode, there can be only one sign-in account.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833516
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class, BrokerHost.class})
@RetryOnFailure
public class TestCase833516 extends AbstractMsalBrokerTest {

    @Test
    public void test_833516() throws MsalException, InterruptedException, LabApiException {
        final String username1 = mLabAccount.getUsername();
        final String password1 = mLabAccount.getPassword();

        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                username1, password1
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        // query to load a user from a same tenant that was used for WPJ
        final LabQuery query = LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();

        final ILabAccount sameTenantUser = mLabClient.getLabAccount(query);
        final String username2 = sameTenantUser.getUsername();
        final String password2 = sameTenantUser.getPassword();

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final TokenRequestLatch latch = new TokenRequestLatch(1);

        // try sign in with an account from the same tenant
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(username2)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .build();
        singleAccountPCA.signIn(signInParameters);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(username2)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingBrokerAccountChooserActivity(false)
                .build();

        AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username2, password2);

        latch.await(TokenRequestTimeout.MEDIUM);

        // try sign in with a different account - it should fail

        // query to load another user from the same tenant
        final LabQuery query2 = LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .protectionPolicy(ProtectionPolicy.MAM_CA)
                .build();

        final ILabAccount difTenantUser = mLabClient.getLabAccount(query2);
        final String difTenantUsername = difTenantUser.getUsername();

        final TokenRequestLatch latch2 = new TokenRequestLatch(1);

        // try sign in with an account from the same tenant
        final SignInParameters signInParameters2 = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(difTenantUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(failureInteractiveCallback(latch2, ErrorCodes.INVALID_PARAMETER))
                .build();
        singleAccountPCA.signIn(signInParameters2);

        latch2.await(TokenRequestTimeout.MEDIUM);
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userRole(UserRole.CLOUD_DEVICE_ADMINISTRATOR)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return null;
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }
}
