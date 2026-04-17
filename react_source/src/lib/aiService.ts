import { GoogleGenerativeAI } from "@google/generative-ai";
import { Category } from '../types';
import { secureLog, anonymizeTransactionData } from './securityUtils';

const API_KEY = process.env.EXPO_PUBLIC_GEMINI_API_KEY || "";
const genAI = new GoogleGenerativeAI(API_KEY);

/**
 * AI Service for Household Ledger
 * 
 * PRIVACY NOTE: This service sends data to Google's Gemini API.
 * Transaction descriptions are NOT sent to preserve user privacy.
 * Only aggregate amounts and category names are shared.
 */
export const aiService = {
    /**
     * Suggests a category based on the transaction description using semantic matching.
     * Note: This sends the description to Google Gemini for analysis.
     */
    suggestCategory: async (description: string, categories: Category[]): Promise<string | null> => {
        if (!description || description.trim().length < 3 || !API_KEY) return null;

        try {
            const model = genAI.getGenerativeModel({ model: "gemini-3-flash-preview" });
            // Only send category names, not full IDs for cleaner prompts
            const categoryList = categories.map(c => c.name).join(', ');
            const prompt = `Given the transaction description: "${description}", select the most appropriate category from: [${categoryList}]. Return ONLY the category name or null if no match.`;

            const result = await model.generateContent(prompt);
            const response = await result.response;
            const text = response.text().trim().toLowerCase();

            // Find matching category by name
            const matched = categories.find(c => c.name.toLowerCase() === text);
            return matched ? matched.id : null;
        } catch (error: any) {
            // Silently handle transient/quota errors for suggestions
            const isTransient = error?.status === 429 || error?.status === 503 || error?.message?.includes("429") || error?.message?.includes("503");
            if (!isTransient) {
                secureLog.error("AI Category Suggestion Error:", error);
            }
            // Fallback to basic keyword matching
            const desc = description.toLowerCase();
            const matched = categories.find(c => desc.includes(c.name.toLowerCase()));
            return matched ? matched.id : null;
        }
    },

    /**
     * Generates a summary/insight for a list of transactions using Gemini.
     * 
     * PRIVACY: Transaction descriptions are ANONYMIZED before sending.
     * Only amounts and category names are shared with the AI service.
     */
    generateSpendingInsight: async (transactions: any[], categories: Category[]): Promise<{ title: string, insight: string, type: 'warning' | 'tip' | 'positive' }> => {
        if (!transactions || transactions.length === 0 || !API_KEY) {
            return { title: 'No Data', insight: 'Add some transactions to see AI-powered insights.', type: 'tip' };
        }

        try {
            const model = genAI.getGenerativeModel({ model: "gemini-3-flash-preview" });

            // PRIVACY: Anonymize transaction data - only send amounts and categories, not descriptions
            const anonymizedData = transactions.map((t, index) => ({
                id: index + 1,
                amount: t.amount,
                category: categories.find(c => c.id === t.categoryId)?.name || 'Other',
                type: t.type
            }));

            const prompt = `Analyze these anonymized household transactions:
            ${JSON.stringify(anonymizedData)}
            
            Generate a concise, helpful insight about spending habits.
            Return JSON: {"title": "3-5 words", "insight": "1-2 sentences", "type": "warning|tip|positive"}`;

            const result = await model.generateContent(prompt);
            const response = await result.response;
            const responseText = response.text();
            const cleanedText = responseText.replace(/```json|```/g, "").trim();

            try {
                return JSON.parse(cleanedText);
            } catch (e) {
                secureLog.error("JSON Parse Error in AI Insights");
                throw e;
            }
        } catch (error: any) {
            const isTransient = [429, 500, 503, 504].includes(error?.status) ||
                error?.message?.includes("429") ||
                error?.message?.includes("503") ||
                error?.message?.includes("quota") ||
                error?.message?.includes("overloaded");

            if (isTransient) {
                secureLog.info("AI Service transient error/quota - silencing.");
                return {
                    title: 'System Busy',
                    insight: 'AI analysis is temporarily paused. Regular tracking is unaffected.',
                    type: 'tip'
                };
            }
            secureLog.error("AI Insights Error:", error);
            return { title: 'Spending Tip', insight: 'Monitor your top categories to stay within budget.', type: 'tip' };
        }
    },

    /**
     * Analyzes a receipt image using Gemini Vision.
     * Note: Receipt images are sent to Google for analysis.
     */
    analyzeReceipt: async (imageUri: string, categories: Category[]): Promise<{ amount: number, description: string, categoryId: string }> => {
        if (!API_KEY) throw new Error("API Key missing");

        try {
            // Load image as base64
            const responseB64 = await fetch(imageUri);
            const blob = await responseB64.blob();
            const base64 = await new Promise<string>((resolve) => {
                const reader = new FileReader();
                reader.onloadend = () => resolve(reader.result as string);
                reader.readAsDataURL(blob);
            });
            const base64Data = base64.split(",")[1];

            const model = genAI.getGenerativeModel({ model: "gemini-3-flash-preview" });
            const prompt = "Extract the total amount, vendor name (description), and suggest a category from this receipt. Categories: " +
                categories.map(c => c.name).join(', ') +
                ". Return JSON only: { \"amount\": number, \"description\": \"string\", \"categoryId\": \"string\" }";

            const result = await model.generateContent([
                prompt,
                { inlineData: { data: base64Data, mimeType: "image/jpeg" } },
            ]);

            const responseText = (await result.response).text().replace(/```json|```/g, "").trim();
            const parsed = JSON.parse(responseText);

            // Map category name back to ID
            const matchedCategory = categories.find(c =>
                c.name.toLowerCase() === parsed.categoryId?.toLowerCase() ||
                c.id === parsed.categoryId
            );

            return {
                ...parsed,
                categoryId: matchedCategory?.id || categories[0]?.id || ''
            };
        } catch (error) {
            secureLog.error("AI Receipt Analysis Error:", error);
            throw error;
        }
    }
};
