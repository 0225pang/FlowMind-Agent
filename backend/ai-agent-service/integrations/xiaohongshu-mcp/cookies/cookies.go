package cookies

import (
	"os"
	"path/filepath"

	"github.com/pkg/errors"
)

type Cookier interface {
	LoadCookies() ([]byte, error)
	SaveCookies(data []byte) error
	DeleteCookies() error
}

func findFlowMindRuntimeCookiesPath() string {
	wd, err := os.Getwd()
	if err != nil {
		return ""
	}
	current := filepath.Clean(wd)
	for {
		if isFlowMindProjectRoot(current) {
			return filepath.Join(current, ".runtime", "xiaohongshu", "cookies.json")
		}
		parent := filepath.Dir(current)
		if parent == current {
			return ""
		}
		current = parent
	}
}

func isFlowMindProjectRoot(path string) bool {
	if info, err := os.Stat(filepath.Join(path, "backend")); err != nil || !info.IsDir() {
		return false
	}
	if info, err := os.Stat(filepath.Join(path, "docs")); err != nil || !info.IsDir() {
		return false
	}
	return true
}

type localCookie struct {
	path string
}

func NewLoadCookie(path string) Cookier {
	if path == "" {
		panic("path is required")
	}

	return &localCookie{
		path: path,
	}
}

// LoadCookies 从文件中加载 cookies。
func (c *localCookie) LoadCookies() ([]byte, error) {

	data, err := os.ReadFile(c.path)
	if err != nil {
		return nil, errors.Wrap(err, "failed to read cookies from tmp file")
	}

	return data, nil
}

// SaveCookies 保存 cookies 到文件中。
func (c *localCookie) SaveCookies(data []byte) error {
	if parent := filepath.Dir(c.path); parent != "." && parent != "" {
		if err := os.MkdirAll(parent, 0755); err != nil {
			return err
		}
	}
	return os.WriteFile(c.path, data, 0644)
}

// DeleteCookies 删除 cookies 文件。
func (c *localCookie) DeleteCookies() error {
	if _, err := os.Stat(c.path); os.IsNotExist(err) {
		// 文件不存在，返回 nil（认为已经删除）
		return nil
	}
	return os.Remove(c.path)
}

// GetCookiesFilePath 获取 cookies 文件路径。
// 为了向后兼容，如果旧路径 /tmp/cookies.json 存在，则继续使用；
// 否则使用当前目录下的 cookies.json
func GetCookiesFilePath() string {
	path := os.Getenv("COOKIES_PATH")
	if path != "" {
		return path
	}
	if projectPath := findFlowMindRuntimeCookiesPath(); projectPath != "" {
		return projectPath
	}

	// 旧路径：/tmp/cookies.json
	tmpDir := os.TempDir()
	oldPath := filepath.Join(tmpDir, "cookies.json")

	// 检查旧路径文件是否存在
	if _, err := os.Stat(oldPath); err == nil {
		// 文件存在，使用旧路径（向后兼容）
		return oldPath
	}

	// 文件不存在，使用新路径（当前目录）
	return "cookies.json"
}
