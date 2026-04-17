import { useSettingsStore } from '../features/settings/store';
import { COLORS_LIGHT, COLORS_DARK } from '../constants/theme';

export const useTheme = () => {
  const isDarkMode = useSettingsStore((state) => state.isDarkMode);
  const colors = isDarkMode ? COLORS_DARK : COLORS_LIGHT;
  return { colors, isDarkMode };
};
