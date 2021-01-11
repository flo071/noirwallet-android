package org.noirofficial.tools.manager;

import static org.noirofficial.tools.manager.PromptManager.PromptItem.FINGER_PRINT;
import static org.noirofficial.tools.manager.PromptManager.PromptItem.PAPER_KEY;
import static org.noirofficial.tools.manager.PromptManager.PromptItem.RECOMMEND_RESCAN;
import static org.noirofficial.tools.manager.PromptManager.PromptItem.UPGRADE_PIN;

import android.content.Context;

import org.noirofficial.NoirWallet;
import org.noirofficial.tools.security.BRKeyStore;
import org.noirofficial.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/18/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class PromptManager {
    public enum PromptItem {SYNCING, FINGER_PRINT, PAPER_KEY, UPGRADE_PIN, RECOMMEND_RESCAN,
        NO_PASS_CODE}

    private static PromptManager instance;

    public static PromptManager getInstance() {
        if (instance == null) {
            instance = new PromptManager();
        }
        return instance;
    }

    private PromptManager() {
    }

    public PromptItem nextPrompt() {
        if (shouldPrompt(RECOMMEND_RESCAN) && !hasPromptBeenDismissed(RECOMMEND_RESCAN)) {
            return RECOMMEND_RESCAN;
        }

        if (shouldPrompt(UPGRADE_PIN) && !hasPromptBeenDismissed(UPGRADE_PIN)) {
            return UPGRADE_PIN;
        }

        if (shouldPrompt(PAPER_KEY) && !hasPromptBeenDismissed(PAPER_KEY)) {
            return PAPER_KEY;
        }

        if (shouldPrompt(FINGER_PRINT) && !hasPromptBeenDismissed(FINGER_PRINT)) {
            return FINGER_PRINT;
        }
        return null;
    }

    private boolean hasPromptBeenDismissed(PromptItem promptItem) {
        switch (promptItem) {
            default:
            case RECOMMEND_RESCAN:
                return BRSharedPrefs.hasPromptDismissed(NoirWallet.getContext(),
                        getPromptName(RECOMMEND_RESCAN));
            case UPGRADE_PIN:
                return BRSharedPrefs.hasPromptDismissed(NoirWallet.getContext(),
                        getPromptName(UPGRADE_PIN));
            case PAPER_KEY:
                return BRSharedPrefs.hasPromptDismissed(NoirWallet.getContext(),
                        getPromptName(PAPER_KEY));
            case FINGER_PRINT:
                return BRSharedPrefs.hasPromptDismissed(NoirWallet.getContext(),
                        getPromptName(FINGER_PRINT));
        }
    }

    private boolean shouldPrompt(PromptItem item) {
        final Context context = NoirWallet.getContext();
        switch (item) {
            case FINGER_PRINT:
                return !BRSharedPrefs.getUseFingerprint(context) && Utils.isFingerprintAvailable(
                        context);
            case PAPER_KEY:
                return !BRSharedPrefs.getPhraseWroteDown(context);
            case UPGRADE_PIN:
                return BRKeyStore.getPinCode(context).length() != 6;
            case RECOMMEND_RESCAN:
                return BRSharedPrefs.getScanRecommended(context);
            default:
                return false;
        }
    }

    /**
     * touchIdPrompt - Shown to the user to enable biometric authentication for purchases under a
     * certain amount.
     * paperKeyPrompt - Shown to the user if they have not yet written down their paper key. This is
     * a persistent prompt and shows up until the user has gone through the paper key flow.
     * upgradePinPrompt - Shown to recommend to the user they should upgrade their PIN from 4 digits
     * to 6. Only shown once. If the user dismisses do not show again.
     * recommendRescanPrompt - Shown when the user should rescan the blockchain
     * noPasscodePrompt - Shown when the user does not have a passcode set up for their device.
     * shareDataPrompt - Shown when asking the user if they wish to share anonymous data. Lowest
     * priority prompt. Only show once and if they dismiss do not show again.
     */
    public String getPromptName(PromptItem prompt) {
        switch (prompt) {
            case FINGER_PRINT:
                return "touchIdPrompt";
            case PAPER_KEY:
                return "paperKeyPrompt";
            case UPGRADE_PIN:
                return "upgradePinPrompt";
            case RECOMMEND_RESCAN:
                return "recommendRescanPrompt";
            case NO_PASS_CODE:
                return "noPasscodePrompt";

        }
        return null;
    }

}
