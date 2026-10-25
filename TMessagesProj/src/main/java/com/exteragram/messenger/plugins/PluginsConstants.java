package com.exteragram.messenger.plugins;

/**
 * Шим строковых констант exteraGram. Значения — те же, что в
 * {@code com.exteragram.messenger.plugins.PluginsConstants} 12.9.0, а не наши
 * внутренние: плагин сравнивает их с тем, что приходит из SDK.
 */
public final class PluginsConstants {

    public static final String PYTHON = com.plusgram.app.plugins.PluginsConstants.PYTHON;
    public static final String SEND_MESSAGE_HOOK = com.plusgram.app.plugins.PluginsConstants.SEND_MESSAGE_HOOK;
    public static final String STRATEGY = com.plusgram.app.plugins.PluginsConstants.STRATEGY;
    public static final String PARAMS = com.plusgram.app.plugins.PluginsConstants.PARAMS;
    public static final String UPDATE = com.plusgram.app.plugins.PluginsConstants.UPDATE;
    public static final String UPDATES = com.plusgram.app.plugins.PluginsConstants.UPDATES;
    public static final String REQUEST = com.plusgram.app.plugins.PluginsConstants.REQUEST;
    public static final String RESPONSE = com.plusgram.app.plugins.PluginsConstants.RESPONSE;
    public static final String ERROR = com.plusgram.app.plugins.PluginsConstants.ERROR;
    public static final String PLUGINS = com.plusgram.app.plugins.PluginsConstants.PLUGINS;
    public static final String PLUGINS_EXT = com.plusgram.app.plugins.PluginsConstants.PLUGINS_EXT;
    public static final String PLUGINS_SDK = com.plusgram.app.plugins.PluginsConstants.PLUGINS_SDK;
    public static final String CREATE_SETTINGS = com.plusgram.app.plugins.PluginsConstants.CREATE_SETTINGS;
    public static final String APP_START = com.plusgram.app.plugins.PluginsConstants.APP_START;
    public static final String APP_STOP = com.plusgram.app.plugins.PluginsConstants.APP_STOP;
    public static final String APP_PAUSE = com.plusgram.app.plugins.PluginsConstants.APP_PAUSE;
    public static final String APP_RESUME = com.plusgram.app.plugins.PluginsConstants.APP_RESUME;
    public static final String ON_APP_EVENT = com.plusgram.app.plugins.PluginsConstants.ON_APP_EVENT;
    public static final String ON_PLUGIN_LOAD = com.plusgram.app.plugins.PluginsConstants.ON_PLUGIN_LOAD;
    public static final String ON_PLUGIN_UNLOAD = com.plusgram.app.plugins.PluginsConstants.ON_PLUGIN_UNLOAD;

    private PluginsConstants() {
    }

    public static final class Settings {
        public static final String TYPE = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE;
        public static final String KEY = com.plusgram.app.plugins.PluginsConstants.Settings.KEY;
        public static final String TEXT = com.plusgram.app.plugins.PluginsConstants.Settings.TEXT;
        public static final String SUBTEXT = com.plusgram.app.plugins.PluginsConstants.Settings.SUBTEXT;
        public static final String ICON = com.plusgram.app.plugins.PluginsConstants.Settings.ICON;
        public static final String ACCENT = com.plusgram.app.plugins.PluginsConstants.Settings.ACCENT;
        public static final String RED = com.plusgram.app.plugins.PluginsConstants.Settings.RED;
        public static final String ON_CLICK = com.plusgram.app.plugins.PluginsConstants.Settings.ON_CLICK;
        public static final String DEFAULT = com.plusgram.app.plugins.PluginsConstants.Settings.DEFAULT;
        public static final String ITEMS = com.plusgram.app.plugins.PluginsConstants.Settings.ITEMS;
        public static final String HINT = com.plusgram.app.plugins.PluginsConstants.Settings.HINT;
        public static final String MULTILINE = com.plusgram.app.plugins.PluginsConstants.Settings.MULTILINE;
        public static final String MAX_LENGTH = com.plusgram.app.plugins.PluginsConstants.Settings.MAX_LENGTH;
        public static final String MASK = com.plusgram.app.plugins.PluginsConstants.Settings.MASK;
        public static final String ON_CHANGE = com.plusgram.app.plugins.PluginsConstants.Settings.ON_CHANGE;
        public static final String TYPE_SWITCH = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_SWITCH;
        public static final String TYPE_INPUT = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_INPUT;
        public static final String TYPE_SELECTOR = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_SELECTOR;
        public static final String TYPE_HEADER = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_HEADER;
        public static final String TYPE_DIVIDER = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_DIVIDER;
        public static final String TYPE_TEXT = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_TEXT;
        public static final String TYPE_EDIT_TEXT = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_EDIT_TEXT;
        public static final String TYPE_CUSTOM = com.plusgram.app.plugins.PluginsConstants.Settings.TYPE_CUSTOM;
        public static final String VIEW = com.plusgram.app.plugins.PluginsConstants.Settings.VIEW;
        public static final String ITEM = com.plusgram.app.plugins.PluginsConstants.Settings.ITEM;
        public static final String FACTORY = com.plusgram.app.plugins.PluginsConstants.Settings.FACTORY;
        public static final String FACTORY_ARGS = com.plusgram.app.plugins.PluginsConstants.Settings.FACTORY_ARGS;
        public static final String CREATE_SUB_FRAGMENT = com.plusgram.app.plugins.PluginsConstants.Settings.CREATE_SUB_FRAGMENT;
        public static final String ON_LONG_CLICK = com.plusgram.app.plugins.PluginsConstants.Settings.ON_LONG_CLICK;
        public static final String LINK_ALIAS = com.plusgram.app.plugins.PluginsConstants.Settings.LINK_ALIAS;

        private Settings() {
        }
    }

    public static final class Strategy {
        public static final String MODIFY = com.plusgram.app.plugins.PluginsConstants.Strategy.MODIFY;
        public static final String CANCEL = com.plusgram.app.plugins.PluginsConstants.Strategy.CANCEL;
        public static final String DEFAULT = com.plusgram.app.plugins.PluginsConstants.Strategy.DEFAULT;
        public static final String MODIFY_FINAL = com.plusgram.app.plugins.PluginsConstants.Strategy.MODIFY_FINAL;

        private Strategy() {
        }
    }

    public static final class Xposed {
        public static final String REPLACE_HOOKED_METHOD = com.plusgram.app.plugins.PluginsConstants.Xposed.REPLACE_HOOKED_METHOD;
        public static final String BEFORE_HOOKED_METHOD = com.plusgram.app.plugins.PluginsConstants.Xposed.BEFORE_HOOKED_METHOD;
        public static final String AFTER_HOOKED_METHOD = com.plusgram.app.plugins.PluginsConstants.Xposed.AFTER_HOOKED_METHOD;
        public static final String HOOK_FILTERS = com.plusgram.app.plugins.PluginsConstants.Xposed.HOOK_FILTERS;

        private Xposed() {
        }
    }

    public static final class DevServer {
        public static final String MODULE = com.plusgram.app.plugins.PluginsConstants.DevServer.MODULE;
        public static final String CLASS = com.plusgram.app.plugins.PluginsConstants.DevServer.CLASS;
        public static final String START_SERVER = com.plusgram.app.plugins.PluginsConstants.DevServer.START_SERVER;
        public static final String STOP_SERVER = com.plusgram.app.plugins.PluginsConstants.DevServer.STOP_SERVER;

        private DevServer() {
        }
    }

    public static final class MenuItemTypes {
        public static final String MESSAGE_CONTEXT_MENU = com.plusgram.app.plugins.PluginsConstants.MenuItemTypes.MESSAGE_CONTEXT_MENU;
        public static final String DRAWER_MENU = com.plusgram.app.plugins.PluginsConstants.MenuItemTypes.DRAWER_MENU;
        public static final String MAIN_MENU = com.plusgram.app.plugins.PluginsConstants.MenuItemTypes.MAIN_MENU;
        public static final String CHAT_ACTION_MENU = com.plusgram.app.plugins.PluginsConstants.MenuItemTypes.CHAT_ACTION_MENU;
        public static final String PROFILE_ACTION_MENU = com.plusgram.app.plugins.PluginsConstants.MenuItemTypes.PROFILE_ACTION_MENU;

        private MenuItemTypes() {
        }
    }

    public static final class MenuItemProperties {
        public static final String MENU_TYPE = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.MENU_TYPE;
        public static final String ITEM_ID = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.ITEM_ID;
        public static final String TEXT = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.TEXT;
        public static final String SUBTEXT = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.SUBTEXT;
        public static final String ICON = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.ICON;
        public static final String ON_CLICK = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.ON_CLICK;
        public static final String CONDITION = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.CONDITION;
        public static final String PRIORITY = com.plusgram.app.plugins.PluginsConstants.MenuItemProperties.PRIORITY;

        private MenuItemProperties() {
        }
    }

    public static final class HookFilterTypes {
        public static final String RESULT_IS_NULL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_IS_NULL;
        public static final String RESULT_IS_TRUE = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_IS_TRUE;
        public static final String RESULT_IS_FALSE = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_IS_FALSE;
        public static final String RESULT_NOT_NULL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_NOT_NULL;
        public static final String RESULT_IS_INSTANCE_OF = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_IS_INSTANCE_OF;
        public static final String RESULT_EQUAL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_EQUAL;
        public static final String RESULT_NOT_EQUAL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.RESULT_NOT_EQUAL;
        public static final String ARGUMENT_IS_NULL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_IS_NULL;
        public static final String ARGUMENT_IS_TRUE = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_IS_TRUE;
        public static final String ARGUMENT_IS_FALSE = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_IS_FALSE;
        public static final String ARGUMENT_NOT_NULL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_NOT_NULL;
        public static final String ARGUMENT_IS_INSTANCE_OF = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_IS_INSTANCE_OF;
        public static final String ARGUMENT_EQUAL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_EQUAL;
        public static final String ARGUMENT_NOT_EQUAL = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.ARGUMENT_NOT_EQUAL;
        public static final String CONDITION = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.CONDITION;
        public static final String OR = com.plusgram.app.plugins.PluginsConstants.HookFilterTypes.OR;

        private HookFilterTypes() {
        }
    }
}
