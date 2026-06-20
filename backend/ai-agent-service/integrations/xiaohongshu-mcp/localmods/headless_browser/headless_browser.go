package headless_browser

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/go-rod/rod"
	"github.com/go-rod/rod/lib/launcher"
	"github.com/go-rod/rod/lib/proto"
	"github.com/go-rod/stealth"
	"github.com/sirupsen/logrus"
)

type Browser struct {
	browser  *rod.Browser
	launcher *launcher.Launcher
}

type Config struct {
	Headless      bool
	UserAgent     string
	Cookies       string
	ChromeBinPath string
	Proxy         string
	Trace         bool
	UserDataDir   string
}

type Option func(*Config)

func newDefaultConfig() *Config {
	return &Config{
		Headless:      true,
		UserAgent:     "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
		Cookies:       "",
		ChromeBinPath: "",
		Trace:         false,
		UserDataDir:   "",
	}
}

func WithHeadless(headless bool) Option {
	return func(c *Config) {
		c.Headless = headless
	}
}

func WithUserAgent(userAgent string) Option {
	return func(c *Config) {
		c.UserAgent = userAgent
	}
}

func WithCookies(cookies string) Option {
	return func(c *Config) {
		c.Cookies = cookies
	}
}

func WithChromeBinPath(path string) Option {
	return func(c *Config) {
		c.ChromeBinPath = path
	}
}

func WithProxy(proxy string) Option {
	return func(c *Config) {
		c.Proxy = proxy
	}
}

func WithTrace() Option {
	return func(c *Config) {
		c.Trace = true
	}
}

func WithUserDataDir(path string) Option {
	return func(c *Config) {
		c.UserDataDir = path
	}
}

func New(options ...Option) *Browser {
	cfg := newDefaultConfig()
	for _, option := range options {
		option(cfg)
	}

	userDataDir := cfg.UserDataDir
	if userDataDir == "" {
		if envDir := os.Getenv("XHS_CHROME_USER_DATA_DIR"); envDir != "" {
			userDataDir = envDir
		}
	}
	if userDataDir == "" {
		if wd, err := os.Getwd(); err == nil {
			userDataDir = filepath.Join(wd, ".chrome-profile")
		} else {
			userDataDir = filepath.Join(os.TempDir(), "flowmind-xhs-chrome-profile")
		}
	}
	userDataDir = filepath.Join(userDataDir, fmt.Sprintf("session-%d-%d", os.Getpid(), time.Now().UnixNano()))
	if err := os.MkdirAll(userDataDir, 0755); err != nil {
		logrus.Warnf("failed to create chrome user data dir %s: %v", userDataDir, err)
	}
	cacheDir := filepath.Join(userDataDir, "cache")
	if err := os.MkdirAll(cacheDir, 0755); err != nil {
		logrus.Warnf("failed to create chrome cache dir %s: %v", cacheDir, err)
	}

	l := launcher.New().
		Leakless(false).
		Headless(cfg.Headless).
		Set("no-sandbox").
		Set("disable-breakpad").
		Set("disable-crash-reporter").
		Set("disable-crashpad").
		Set("disable-background-networking").
		Set("disable-component-update").
		Set("no-first-run").
		Set("no-default-browser-check").
		Set("disable-dev-shm-usage").
		Set("disable-gpu").
		Set("disable-gpu-shader-disk-cache").
		Set("disable-features", "Crashpad").
		Set("remote-debugging-port", "0").
		Set("disk-cache-dir", cacheDir).
		Set("user-data-dir", userDataDir).
		Set("user-agent", cfg.UserAgent)

	if cfg.ChromeBinPath != "" {
		l = l.Bin(cfg.ChromeBinPath)
	}

	if cfg.Proxy != "" {
		l = l.Proxy(cfg.Proxy)
	}

	url := l.MustLaunch()
	browser := rod.New().
		ControlURL(url).
		Trace(cfg.Trace).
		MustConnect()

	if cfg.Cookies != "" {
		var cookies []*proto.NetworkCookie
		if err := json.Unmarshal([]byte(cfg.Cookies), &cookies); err != nil {
			logrus.Warnf("failed to unmarshal cookies: %v", err)
		} else {
			browser.MustSetCookies(cookies...)
		}
	}

	return &Browser{
		browser:  browser,
		launcher: l,
	}
}

func (b *Browser) Close() {
	b.browser.MustClose()
	b.launcher.Cleanup()
}

func (b *Browser) NewPage() *rod.Page {
	return stealth.MustPage(b.browser)
}
