/**
 * Highlight zone updates in different colors
 */

Tapestry.ElementEffect.colorhighlight = function(element, color) 
{
    if (color)
        return new Effect.Highlight(element, {
            startcolor : '%s',
            endcolor: color,
            restorecolor: color
        });
    return new Effect.Highlight(element, { 
        startcolor : '%s',
        restorecolor: $(element).savedBackgroundColor
        }
    );
};

// override default Tapestry highlight
Tapestry.ElementEffect.taphighlight = Tapestry.ElementEffect.highlight;  // save the original function
Tapestry.ElementEffect.highlight = Tapestry.ElementEffect.colorhighlight;
